package no.nav.fo.veilarbdialog.service;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.fo.veilarbdialog.db.dao.VarselDAO;
import no.nav.fo.veilarbdialog.util.FunksjonelleMetrikker;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.StoppReVarsel;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarsel;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarslingstyper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import java.time.Instant;
import java.util.List;

import static java.util.UUID.randomUUID;
import static no.nav.fo.veilarbdialog.util.MessageQueueUtils.jaxbContext;

@Slf4j
@Component
public class ScheduleRessurs {

    private static final long GRACE_PERIODE = 30 * 60 * 1000;

    static final JAXBContext VARSEL_CONTEXT = jaxbContext(XMLVarsel.class, XMLVarslingstyper.class);
    static final JAXBContext STOPP_VARSEL_CONTEXT = jaxbContext(StoppReVarsel.class);

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
    private LockingTaskExecutor lockingTaskExecutor;

    @Scheduled(cron = "0 0/2 * * * *")
    public void sjekkForVarsel() {
        lockingTaskExecutor.executeWithLock(this::sjekkForVarselWithLock,
                new LockConfiguration("varsel", Instant.now().plusSeconds(90)));
    }

    private void sjekkForVarselWithLock() {
        List<String> varselUUIDer = varselDAO.hentRevarslerSomSkalStoppes();
        log.info("Stopper {} revarsler", varselUUIDer.size());
        varselUUIDer.forEach(stopRevarslingService::stopRevarsel);
        FunksjonelleMetrikker.stoppetRevarsling(varselUUIDer.size());

        List<String> aktorer = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(GRACE_PERIODE);
        log.info("Varsler {} brukere", aktorer.size());
        long paragraf8Varsler = aktorer
                .stream()
                .map(this::sendVarsel)
                .filter(varselType -> varselType == VarselType.PARAGRAF8)
                .count();

        log.info("Varsler sendt, {} paragraf8", paragraf8Varsler);
        FunksjonelleMetrikker.nyeVarsler(aktorer.size(), paragraf8Varsler);
    }


    private VarselType sendVarsel(String aktorId) {
        boolean paragraf8 = varselDAO.harUlesteUvarsledeParagraf8Henvendelser(aktorId);
        if (paragraf8) {
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
