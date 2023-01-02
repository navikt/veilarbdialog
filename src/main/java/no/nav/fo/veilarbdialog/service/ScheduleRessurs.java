package no.nav.fo.veilarbdialog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
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
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerKanIkkeVarslesException;
import no.nav.fo.veilarbdialog.metrics.FunksjonelleMetrikker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    // Ti min
    @Scheduled(fixedDelay = 600000)
    public void slettGamleKladder() {
        kladdService.slettGamleKladder();
    }

    //5MIN ER VALGT ARBITRÆRT
    @Scheduled(fixedDelay = 300000)
    public void sendFeilendeKafkaMeldinger() {
        kafkaProducerService.sendAlleFeilendeMeldinger();
    }

    // To minutter mellom hver kjøring
    @Scheduled(initialDelay = 60000, fixedDelay = 120000)
    @Transactional
    @SchedulerLock(name = "brukernotifikasjon_beskjed_kafka_scheduledTask", lockAtMostFor = "PT2M")
    public void sendBrukernotifikasjonerForUlesteDialoger() {
        List<Long> dialogIder = varselDAO.hentDialogerMedUlesteMeldingerEtterSisteVarsel(brukernotifikasjonGracePeriode, brukernotifikasjonHenvendelseMaksAlder);

        log.info("Varsler (beskjed): {} brukere", dialogIder.size());

        dialogIder.forEach(
                dialogId -> {
                    DialogData dialogData = dialogDAO.hentDialog(dialogId);
                    Fnr fnr = aktorOppslagClient.hentFnr(AktorId.of(dialogData.getAktorId()));

                    UUID oppfolgingsperiode = dialogData.getOppfolgingsperiode();

                    Brukernotifikasjon brukernotifikasjon = new Brukernotifikasjon(
                            UUID.randomUUID(),
                            dialogData.getId(),
                            fnr,
                            BrukernotifikasjonTekst.BESKJED_BRUKERNOTIFIKASJON_TEKST,
                            oppfolgingsperiode,
                            BrukernotifikasjonsType.BESKJED,
                            dialogDataService.utledDialogLink(dialogId)
                    );

                    try {
                        brukernotifikasjonService.bestillBrukernotifikasjon(brukernotifikasjon, AktorId.of(dialogData.getAktorId()));
                    } catch (BrukerKanIkkeVarslesException e) {
                        log.warn("Bruker kan ikke varsles.");
                        funksjonelleMetrikker.nyBrukernotifikasjon(false, BrukernotifikasjonsType.BESKJED);
                    }
                    funksjonelleMetrikker.nyBrukernotifikasjon(true, BrukernotifikasjonsType.BESKJED);
                }
        );
    }

}
