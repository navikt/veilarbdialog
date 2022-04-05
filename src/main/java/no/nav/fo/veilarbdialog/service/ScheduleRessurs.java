package no.nav.fo.veilarbdialog.service;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleRessurs {

    private final DialogDAO dialogDAO;
    private final VarselDAO varselDAO;
    private final KladdService kladdService;
    private final DialogDataService dialogDataService;
    private final LockingTaskExecutor lockingTaskExecutor;
    private final KafkaProducerService kafkaProducerService;
    private final FunksjonelleMetrikker funksjonelleMetrikker;
    private final BrukernotifikasjonService brukernotifikasjonService;
    private final AktorOppslagClient aktorOppslagClient;

    @Value("${application.brukernotifikasjon.grace.periode.ms}")
    private Long brukernotifikasjonGracePeriode;

    @Value("${application.brukernotifikasjon.henvendelse.maksalder.ms}")
    private Long brukernotifikasjonHenvendelseMaksAlder;

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

    private void sendBrukernotifikasjonerForUlesteDialogerWithLock() {
        List<Long> dialogIder = varselDAO.hentDialogerMedUlesteMeldingerEtterSisteVarsel(brukernotifikasjonGracePeriode, brukernotifikasjonHenvendelseMaksAlder);

        log.info("Varsler (beskjed): {} brukere", dialogIder.size());

        dialogIder.forEach(
                dialogId -> {
                    DialogData dialogData = dialogDAO.hentDialog(dialogId);
                    Fnr fnr = aktorOppslagClient.hentFnr(AktorId.of(dialogData.getAktorId()));

                    boolean kanVarsles = brukernotifikasjonService.kanVarsles(fnr);
                    if (!kanVarsles) {
                        log.warn("Kan ikke varsle bruker: {}. Se årsak i SecureLog", dialogData.getAktorId());
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
                    funksjonelleMetrikker.nyBrukernotifikasjon(true, BrukernotifikasjonsType.BESKJED);
                }
        );
    }

}
