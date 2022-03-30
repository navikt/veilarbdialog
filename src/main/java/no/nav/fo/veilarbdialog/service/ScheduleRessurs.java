package no.nav.fo.veilarbdialog.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.brukernotifikasjon.Brukernotifikasjon;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonTekst;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.VarselDAO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.metrics.FunksjonelleMetrikker;
import no.nav.fo.veilarbdialog.oppfolging.siste_periode.SistePeriodeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleRessurs {

    private final DialogDAO dialogDAO;
    private final VarselDAO varselDAO;
    private final KladdService kladdService;
    private final DialogDataService dialogDataService;
    private final ServiceMeldingService serviceMeldingService;
    private final OppgaveService oppgaveService;
    private final StopRevarslingService stopRevarslingService;
    private final VarselMedHandlingService varselMedHandlingService;
    private final LockingTaskExecutor lockingTaskExecutor;
    private final KafkaProducerService kafkaProducerService;
    private final MeterRegistry registry;
    private final FunksjonelleMetrikker funksjonelleMetrikker;
    private final BrukernotifikasjonService brukernotifikasjonService;
    private final SistePeriodeService sistePeriodeService;
    private final AktorOppslagClient aktorOppslagClient;

    @Value("${application.brukernotifikasjon.grace.periode.ms}")
    private Long brukernotifikasjonGracePeriode;

    @Scheduled(cron = "0 0/10 * * * *")
    public void slettGamleKladder() {
        kladdService.slettGamleKladder();
    }

    //5MIN ER VALGT ARBITRÆRT
    @Scheduled(cron = "0 0/5 * * * *")
    public void sendFeilendeKafkaMeldinger() {
        kafkaProducerService.sendAlleFeilendeMeldinger();
    }

    @Scheduled(cron = "0 0/2 * * * *")
    @Transactional
    public void sendBrukernotifikasjonerForUlesteDialoger() {
        lockingTaskExecutor.executeWithLock(
                (Runnable) this::sendBrukernotifikasjonerForUlesteDialogerWithLock,
                new LockConfiguration(Instant.now(), "varsel", Duration.ofMinutes(30), Duration.ZERO)
        );
    }

//    @Scheduled(cron = "0 0/2 * * * *")
//    public void sjekkForVarsel() {
//        lockingTaskExecutor.executeWithLock(
//                (Runnable) this::sjekkForVarselWithLock,
//                new LockConfiguration(Instant.now(), "varsel", Duration.ofMinutes(30), Duration.ZERO)
//        );
//    }

    private void sendBrukernotifikasjonerForUlesteDialogerWithLock() {
        List<Long> dialogIder = varselDAO.hentDialogerMedUlesteMeldingerEtterSisteVarsel(brukernotifikasjonGracePeriode);

        log.info("Varsler (beskjed): {} brukere", dialogIder.size());

        dialogIder.forEach(
                dialogId -> {
                    DialogData dialogData = dialogDAO.hentDialog(dialogId);
                    Fnr fnr = aktorOppslagClient.hentFnr(AktorId.of(dialogData.getAktorId()));

                    boolean kanVarsles = brukernotifikasjonService.kanVarsles(fnr);
                    if (!kanVarsles) {
                        log.warn("Kan ikke varsle bruker: {}", dialogData.getAktorId());
                        funksjonelleMetrikker.nyBrukernotifikasjon(false, BrukernotifikasjonsType.BESKJED);
                        return;
                    }

                    UUID oppfolgingsperiode = dialogData.getOppfolgingsperiode();

                    Brukernotifikasjon brukernotifikasjon = new Brukernotifikasjon(
                            UUID.randomUUID(),
                            dialogData.getId(),
                            fnr,
                            BrukernotifikasjonTekst.BESKJED_BRUKERNOTIFIKASJON_TEKST,
                            oppfolgingsperiode,
                            BrukernotifikasjonsType.BESKJED,
                            BrukernotifikasjonTekst.BESKJED_EPOST_TITTEL,
                            BrukernotifikasjonTekst.BESKJED_EPOST_BODY,
                            BrukernotifikasjonTekst.BESKJED_SMS_TEKST,
                            dialogDataService.utledDialogLink(dialogId)
                    );

                    brukernotifikasjonService.bestillBrukernotifikasjon(brukernotifikasjon, AktorId.of(dialogData.getAktorId()));
                    varselDAO.oppdaterSisteVarselForBruker(dialogData.getAktorId());
                    funksjonelleMetrikker.nyBrukernotifikasjon(true, BrukernotifikasjonsType.BESKJED);
                }
        );
    }



    private void sjekkForVarselWithLock() {

        Timer timer = registry.timer("dialog.varsel.timer");
        timer.record(() -> {

            List<String> varselUUIDer = varselDAO.hentRevarslerSomSkalStoppes();
            log.info("Stopper {} revarsler", varselUUIDer.size());
            varselUUIDer.forEach(stopRevarslingService::stopRevarsel);
            funksjonelleMetrikker.stoppetRevarsling(varselUUIDer.size());

            List<String> aktorer = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(Duration.ofMinutes(30).toMillis());

            log.info("Varsler {} brukere", aktorer.size());
            log.info("Varsler aktorer: " + aktorer);
            long paragraf8Varsler = aktorer
                    .stream()
                    .map(this::sendVarsel)
                    .filter(varselType -> varselType == VarselType.PARAGRAF8)
                    .count();

            log.info("Varsler sendt, {} paragraf8", paragraf8Varsler);
            funksjonelleMetrikker.nyeVarsler(aktorer.size(), paragraf8Varsler);

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
