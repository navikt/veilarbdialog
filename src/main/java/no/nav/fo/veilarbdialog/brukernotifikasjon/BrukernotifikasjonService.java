package no.nav.fo.veilarbdialog.brukernotifikasjon;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.brukernotifikasjon.entity.BrukernotifikasjonEntity;
import no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering.KvitteringDAO;
import no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering.KvitteringMetrikk;
import no.nav.fo.veilarbdialog.clients.veilarboppfolging.ManuellStatusV2DTO;
import no.nav.fo.veilarbdialog.db.dao.VarselDAO;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerKanIkkeVarslesException;
import no.nav.fo.veilarbdialog.minsidevarsler.DialogVarsel;
import no.nav.fo.veilarbdialog.minsidevarsler.VarselInaktivering;
import no.nav.fo.veilarbdialog.oppfolging.v2.OppfolgingV2Client;
import no.nav.fo.veilarbdialog.minsidevarsler.MinsideVarselProducer;
import no.nav.fo.veilarbdialog.minsidevarsler.PendingVarsel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BrukernotifikasjonService {
    private final Logger secureLogs = LoggerFactory.getLogger("SecureLog");

    private final OppfolgingV2Client oppfolgingClient;
    private final BrukernotifikasjonRepository brukernotifikasjonRepository;
    private final VarselDAO varselDAO;
    private final KvitteringDAO kvitteringDAO;
    private final KvitteringMetrikk kvitteringMetrikk;
    private final MinsideVarselProducer minsideVarselProducer;

    public BrukernotifikasjonEntity bestillVarsel(DialogVarsel brukernotifikasjon, AktorId aktorId) {
        BrukernotifikasjonInsert insert = new BrukernotifikasjonInsert(
                brukernotifikasjon.getVarselId(),
                brukernotifikasjon.getDialogId(),
                brukernotifikasjon.getFoedselsnummer(),
                brukernotifikasjon.getMelding(),
                brukernotifikasjon.getOppfolgingsperiodeId(),
                brukernotifikasjon.getType(),
                BrukernotifikasjonBehandlingStatus.PENDING,
                brukernotifikasjon.getLink()
        );
        if (!kanVarsles(brukernotifikasjon.getFoedselsnummer())) {
            log.warn("Kan ikke varsle bruker: {}. Se årsak i SecureLog", aktorId.get());
            throw new BrukerKanIkkeVarslesException();
        }

        Long id = brukernotifikasjonRepository.opprettBrukernotifikasjon(insert);
        varselDAO.oppdaterSisteVarselForBruker(aktorId.get());
        return hentBrukernotifikasjon(id);
    }

    public void bestillDone(long brukernotifikasjonId) {
        brukernotifikasjonRepository.updateStatus(brukernotifikasjonId, BrukernotifikasjonBehandlingStatus.SKAL_AVSLUTTES);
    }

    @Transactional
    public void bestillDoneForOppfolgingsperiode(UUID oppfolgingsperiode) {
        brukernotifikasjonRepository.bestillDoneForPeriode(oppfolgingsperiode);
    }

    @Scheduled(
            initialDelay = 60000,
            fixedDelay = 5000
    )
    @SchedulerLock(name = "brukernotifikasjon_oppgave_kafka_scheduledTask", lockAtMostFor = "PT2M")
    public void sendPendingBrukernotifikasjoner() {
        List<BrukernotifikasjonEntity> pendingBrukernotifikasjoner = brukernotifikasjonRepository.hentPendingBrukernotifikasjoner();
        pendingBrukernotifikasjoner.stream().forEach( brukernotifikasjonEntity -> {
                    minsideVarselProducer.publiserVarselPåKafka(new PendingVarsel(
                            brukernotifikasjonEntity.varselId(),
                            brukernotifikasjonEntity.melding(),
                            brukernotifikasjonEntity.lenke(),
                            brukernotifikasjonEntity.type(),
                            brukernotifikasjonEntity.fnr()
                    ));
                    brukernotifikasjonRepository.updateStatus(brukernotifikasjonEntity.id(), BrukernotifikasjonBehandlingStatus.SENDT);
                }
        );
    }

    @Scheduled(
            initialDelay = 60000,
            fixedDelay = 5000
    )
    @SchedulerLock(name = "brukernotifikasjon_done_kafka_scheduledTask", lockAtMostFor = "PT2M")
    public void sendDoneBrukernotifikasjoner() {
        List<BrukernotifikasjonEntity> skalAvsluttesNotifikasjoner = brukernotifikasjonRepository.hentPendingDoneBrukernotifikasjoner();
        skalAvsluttesNotifikasjoner.stream().forEach(
                brukernotifikasjonEntity ->  {
                    var inaktivering = new VarselInaktivering(brukernotifikasjonEntity.varselId());
                    minsideVarselProducer.publiserInaktiveringsMeldingPåKafka(inaktivering);
                    brukernotifikasjonRepository.updateStatus(brukernotifikasjonEntity.id(), BrukernotifikasjonBehandlingStatus.AVSLUTTET);
                }
        );
    }

    @Scheduled(
            initialDelay = 60000,
            fixedDelay = 30000
    )
    public void countForsinkedeVarslerSisteDognet() {
        int antall = kvitteringDAO.hentAntallUkvitterteVarslerForsoktSendt(20);
        kvitteringMetrikk.countForsinkedeVarslerSisteDognet(antall);
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

    public BrukernotifikasjonEntity hentBrukernotifikasjon(long brukernotifikasjonId) {
        return brukernotifikasjonRepository.hentBrukernotifikasjon(brukernotifikasjonId).orElseThrow();
    }

}
