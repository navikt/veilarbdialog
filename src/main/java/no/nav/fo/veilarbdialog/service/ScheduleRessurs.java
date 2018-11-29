package no.nav.fo.veilarbdialog.service;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.fo.veilarbdialog.db.dao.VarselDAO;
import no.nav.fo.veilarbdialog.util.FunksjonelleMetrikker;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.StoppReVarsel;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarsel;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarslingstyper;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.time.Instant;
import java.util.List;

import static java.util.UUID.randomUUID;
import static javax.xml.bind.JAXBContext.newInstance;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class ScheduleRessurs {

    private static final Logger LOG = getLogger(ScheduleRessurs.class);

    private static final long GRACE_PERIODE = 30 * 60 * 1000;

    static final JAXBContext VARSEL_CONTEXT;
    static final JAXBContext STOPP_VARSEL_CONTEXT;

    static {
        try {
            VARSEL_CONTEXT = newInstance(
                    XMLVarsel.class,
                    XMLVarslingstyper.class
            );
            STOPP_VARSEL_CONTEXT = newInstance(
                    StoppReVarsel.class
            );
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    @Inject
    private VarselDAO varselDAO;

    @Inject
    private ServiceMeldingService serviceMeldingService;

    @Inject
    private OppgaveService oppgaveService;

    @Inject
    private StopRevarslingService stopRevarslingService;

    @Inject
    private VarselMedHandlingService varselMedHandlingService;

    @Inject
    private UnleashService unleashService;

    @Inject
    private LockingTaskExecutor lockingTaskExecutor;

    @Scheduled(cron = "0 0/2 * * * *")
    public void sjekkForVarsel() {
        lockingTaskExecutor.executeWithLock(this::sjekkForVarselWithLock,
                new LockConfiguration("varsel", Instant.now().plusSeconds(90)));
    }

    private void sjekkForVarselWithLock() {
        List<String> varselUUIDer = varselDAO.hentRevarslerSomSkalStoppes();
        LOG.info("Stopper {} revarsler", varselUUIDer.size());
        varselUUIDer.forEach(stopRevarslingService::stopRevarsel);
        FunksjonelleMetrikker.stoppetRevarsling(varselUUIDer.size());

        List<String> aktorer = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(GRACE_PERIODE);
        LOG.info("Varsler {} brukere", aktorer.size());
        long paragraf8Varsler = aktorer
                .stream()
                .map(this::sendVarsel)
                .filter(varselType -> varselType == VarselType.PARAGRAF8)
                .count();

        LOG.info("Varsler sendt, {} paragraf8", paragraf8Varsler);
        FunksjonelleMetrikker.nyeVarsler(aktorer.size(), paragraf8Varsler);
    }


    private VarselType sendVarsel(String aktorId) {
        boolean paragraf8 = varselDAO.harUlesteUvarsledeParagraf8Henvendelser(aktorId);
        boolean enabled = unleashService.isEnabled("veilarbdialog-send-paragraf8");
        if (paragraf8 && enabled) {
            String varselBestillingId = randomUUID().toString();

            varselMedHandlingService.send(aktorId, varselBestillingId);
            oppgaveService.send(aktorId, varselBestillingId);

            varselDAO.insertParagraf8Varsel(aktorId, varselBestillingId);
            varselDAO.setVarselUUIDForParagraf8Dialoger(aktorId, varselBestillingId);
        } else {
            serviceMeldingService.sendVarsel(aktorId);
        }
        varselDAO.oppdaterSisteVarselForBruker(aktorId);
        return paragraf8 ? VarselType.PARAGRAF8 : VarselType.DIALOG;
    }

    private enum VarselType {
        PARAGRAF8,
        DIALOG
    }

}
