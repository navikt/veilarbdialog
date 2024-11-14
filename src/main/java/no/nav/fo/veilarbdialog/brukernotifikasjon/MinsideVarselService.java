package no.nav.fo.veilarbdialog.brukernotifikasjon;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.brukernotifikasjon.entity.BrukernotifikasjonEntity;
import no.nav.fo.veilarbdialog.clients.veilarboppfolging.ManuellStatusV2DTO;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerKanIkkeVarslesException;
import no.nav.fo.veilarbdialog.minsidevarsler.DialogVarsel;
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinSideVarselId;
import no.nav.fo.veilarbdialog.oppfolging.v2.OppfolgingV2Client;
import no.nav.fo.veilarbdialog.minsidevarsler.MinsideVarselProducer;
import no.nav.fo.veilarbdialog.minsidevarsler.PendingVarsel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonBehandlingStatus.PENDING;

@Service
@Slf4j
@RequiredArgsConstructor
public class MinsideVarselService {
    private final Logger secureLogs = LoggerFactory.getLogger("SecureLog");

    private final OppfolgingV2Client oppfolgingClient;
    private final BrukernotifikasjonRepository brukernotifikasjonRepository;
    private final MinsideVarselProducer minsideVarselProducer;

    public BrukernotifikasjonEntity puttVarselIOutbox(DialogVarsel varsel, AktorId aktorId) {
        if (!kanVarsles(varsel.getFoedselsnummer())) {
            log.warn("Kan ikke varsle bruker: {}. Se årsak i SecureLog", aktorId.get());
            throw new BrukerKanIkkeVarslesException();
        }

        List<BrukernotifikasjonEntity> eksisterendeVarsel = brukernotifikasjonRepository.hentBrukernotifikasjonForDialogId(varsel.getDialogId(), varsel.getType());
        if (!eksisterendeVarsel.isEmpty()) {
            // Hvis det er sendt eller skal sendes varsel for denne dialogen siste halvtimen, ikke opprett nytt varsel
            var uteståendeVarslerForDialogId = eksisterendeVarsel.stream().anyMatch(
                    it -> (it.status() == PENDING ||  it.status() == BrukernotifikasjonBehandlingStatus.SENDT)
                            && it.opprettet().isAfter(LocalDateTime.now().minusMinutes(30)));
            if (uteståendeVarslerForDialogId) return null;
        }
        Long id = brukernotifikasjonRepository.opprettVarselIPendingStatus(varsel);
        log.info("Minside varsel opprettet i PENDING status {}", varsel.getVarselId());
        return hentBrukernotifikasjon(id);
    }

    public void setVarselTilSkalAvsluttes(long brukernotifikasjonId) {
        brukernotifikasjonRepository.hentBrukernotifikasjon(brukernotifikasjonId)
                .map(BrukernotifikasjonEntity::varselId)
                .ifPresent(this::setVarselTilSkalAvsluttes);
    }

    public void setVarselTilSkalAvsluttes(MinSideVarselId varselId) {
        brukernotifikasjonRepository.updateStatus(varselId, BrukernotifikasjonBehandlingStatus.SKAL_AVSLUTTES);
    }

    @Transactional
    public void setSkalAvsluttesForVarslerIPeriode(UUID oppfolgingsperiode) {
        brukernotifikasjonRepository.setSkalAvsluttesForVarslerIPeriode(oppfolgingsperiode);
    }

    @Scheduled(
            initialDelay = 60000,
            fixedDelay = 5000
    )
    @SchedulerLock(name = "varsler_oppgave_kafka_scheduledTask", lockAtMostFor = "PT2M")
    public void sendPendingVarsler() {
        List<BrukernotifikasjonEntity> pendingBrukernotifikasjoner = brukernotifikasjonRepository.hentPendingVarsler();
        pendingBrukernotifikasjoner.stream().forEach( brukernotifikasjonEntity -> {
                    minsideVarselProducer.publiserVarselPåKafka(new PendingVarsel(
                            brukernotifikasjonEntity.varselId(),
                            brukernotifikasjonEntity.melding(),
                            brukernotifikasjonEntity.lenke(),
                            brukernotifikasjonEntity.type(),
                            brukernotifikasjonEntity.fnr(),
                            brukernotifikasjonEntity.skalBatches()
                    ));
                    brukernotifikasjonRepository.updateStatus(brukernotifikasjonEntity.varselId(), BrukernotifikasjonBehandlingStatus.SENDT);
                }
        );
    }

    @Scheduled(
            initialDelay = 60000,
            fixedDelay = 5000
    )
    @SchedulerLock(name = "varsel_inaktivering_kafka_scheduledTask", lockAtMostFor = "PT2M")
    public void sendInktiveringPåKafkaPåVarslerSomSkalAvsluttes() {
        List<BrukernotifikasjonEntity> skalAvsluttesNotifikasjoner = brukernotifikasjonRepository.hentVarslerSomSkalAvsluttes();
        skalAvsluttesNotifikasjoner.stream().forEach( varselSomSkalAvsluttes ->  {
                    minsideVarselProducer.publiserInaktiveringsMeldingPåKafka(varselSomSkalAvsluttes.varselId());
                    brukernotifikasjonRepository.updateStatus(varselSomSkalAvsluttes.varselId(), BrukernotifikasjonBehandlingStatus.AVSLUTTET);
                }
        );
    }

    public boolean kanVarsles(Fnr fnr) {
        Optional<ManuellStatusV2DTO> manuellStatusResponse = oppfolgingClient.hentManuellStatus(fnr);
        boolean erManuell = manuellStatusResponse.map(ManuellStatusV2DTO::isErUnderManuellOppfolging).orElse(true);
        boolean erReservertIKrr = manuellStatusResponse.map(ManuellStatusV2DTO::getKrrStatus).map(ManuellStatusV2DTO.KrrStatus::isErReservert).orElse(true);

        boolean kanVarsles = !erManuell && !erReservertIKrr;

        if(!kanVarsles) {
            secureLogs.warn("bruker med fnr: {} kan ikke varsles, statuser erManuell: {}, erReservertIKrr: {}}", fnr, erManuell, erReservertIKrr);
        }

        return kanVarsles;
    }

    private BrukernotifikasjonEntity hentBrukernotifikasjon(long brukernotifikasjonId) {
        return brukernotifikasjonRepository.hentBrukernotifikasjon(brukernotifikasjonId).orElseThrow();
    }

}
