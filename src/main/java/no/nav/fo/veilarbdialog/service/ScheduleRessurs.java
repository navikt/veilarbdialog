package no.nav.fo.veilarbdialog.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.fo.veilarbdialog.db.dao.VarselDAO;
import no.nav.fo.veilarbdialog.util.FunksjonelleMetrikker;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static java.util.UUID.randomUUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleRessurs {

    private static final long GRACE_PERIODE = 30 * 60 * 1000;

    private final VarselDAO varselDAO;
    private final KladdService kladdService;
    private final ServiceMeldingService serviceMeldingService;
    private final OppgaveService oppgaveService;
    private final StopRevarslingService stopRevarslingService;
    private final VarselMedHandlingService varselMedHandlingService;
    private final LockingTaskExecutor lockingTaskExecutor;
    private final KafkaDialogService kafkaDialogService;
    private final MeterRegistry registry;

    @Scheduled(cron = "0 0/10 * * * *")
    public void slettGamleKladder() {
        kladdService.slettGamleKladder();
    }

    //5MIN ER VALGT ARBITRÆRT
    @Scheduled(cron = "0 0/5 * * * *")
    public void sendFeilendeKafkaMeldinger() {
        kafkaDialogService.sendAlleFeilendeMeldinger();
    }

    @Scheduled(cron = "0 0/2 * * * *")
    public void sjekkForVarsel() {
        lockingTaskExecutor.executeWithLock(
                (Runnable) this::sjekkForVarselWithLock,
                new LockConfiguration(Instant.now(), "varsel", Duration.ofSeconds(60 * 30), Duration.ZERO)
        );
    }

    private void sjekkForVarselWithLock() {

        Timer timer = registry.timer("dialog.varsel.timer");
        timer.record(() -> {

            List<String> varselUUIDer = varselDAO.hentRevarslerSomSkalStoppes();
            log.info("Stopper {} revarsler", varselUUIDer.size());
            varselUUIDer.forEach(stopRevarslingService::stopRevarsel);
            FunksjonelleMetrikker.stoppetRevarsling(varselUUIDer.size());

            List<String> aktorer = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(GRACE_PERIODE);
            log.info("Varsler {} brukere", aktorer.size());
            log.info("Varsler aktorer: " + aktorer);
            long paragraf8Varsler = aktorer
                    .stream()
                    .map(this::sendVarsel)
                    .filter(varselType -> varselType == VarselType.PARAGRAF8)
                    .count();

            log.info("Varsler sendt, {} paragraf8", paragraf8Varsler);
            FunksjonelleMetrikker.nyeVarsler(aktorer.size(), paragraf8Varsler);

        });

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
