package no.nav.fo.veilarbdialog.brukernotifikasjon;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.brukernotifikasjon.schemas.builders.DoneInputBuilder;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.brukernotifikasjon.entity.BrukernotifikasjonEntity;
import no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering.KvitteringDAO;
import no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering.KvitteringMetrikk;
import no.nav.fo.veilarbdialog.clients.veilarboppfolging.ManuellStatusV2DTO;
import no.nav.fo.veilarbdialog.db.dao.VarselDAO;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerKanIkkeVarslesException;
import no.nav.fo.veilarbdialog.oppfolging.v2.OppfolgingV2Client;
import no.nav.fo.veilarbdialog.minsidevarsler.MinsideVarselProducer;
import no.nav.fo.veilarbdialog.minsidevarsler.PendingVarsel;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class BrukernotifikasjonService {
    private final Logger secureLogs = LoggerFactory.getLogger("SecureLog");

    private static final int OPPGAVE_SIKKERHETSNIVAA = 4;
    private static final int BESKJED_SIKKERHETSNIVAA = 3;

    private final OppfolgingV2Client oppfolgingClient;
    private final BrukernotifikasjonRepository brukernotifikasjonRepository;
    private final KafkaTemplate<NokkelInput, OppgaveInput> kafkaOppgaveProducer;
    private final KafkaTemplate<NokkelInput, BeskjedInput> kafkaBeskjedProducer;
    private final KafkaTemplate<NokkelInput, DoneInput> kafkaDoneProducer;
    private final VarselDAO varselDAO;
    private final KvitteringDAO kvitteringDAO;
    private final KvitteringMetrikk kvitteringMetrikk;
    private final MinsideVarselProducer minsideVarselProducer;

    @Value("${application.topic.ut.brukernotifikasjon.oppgave}")
    private String oppgaveTopic;
    @Value("${application.topic.ut.brukernotifikasjon.beskjed}")
    private String beskjedTopic;
    @Value("${application.topic.ut.brukernotifikasjon.done}")
    private String doneTopic;
    @Value("${spring.application.name}")
    String applicationName;
    @Value("${application.namespace}")
    String namespace;

    public BrukernotifikasjonEntity bestillBrukernotifikasjon(Brukernotifikasjon brukernotifikasjon, AktorId aktorId) {
        BrukernotifikasjonInsert insert = new BrukernotifikasjonInsert(
                brukernotifikasjon.eventId(),
                brukernotifikasjon.dialogId(),
                brukernotifikasjon.foedselsnummer(),
                brukernotifikasjon.melding(),
                brukernotifikasjon.oppfolgingsperiodeId(),
                brukernotifikasjon.type(),
                BrukernotifikasjonBehandlingStatus.PENDING,
                brukernotifikasjon.link()
        );
        if (!kanVarsles(brukernotifikasjon.foedselsnummer())) {
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
                            brukernotifikasjonEntity.eventId(),
                            brukernotifikasjonEntity.melding(),
//                            brukernotifikasjonEntity.oppfolgingsPeriodeId().toString(),
                            brukernotifikasjonEntity.epostTittel(),
                            brukernotifikasjonEntity.epostBody(),
                            brukernotifikasjonEntity.smsText(),
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
                    DoneInfo doneInfo = new DoneInfo(
                            ZonedDateTime.now(ZoneOffset.UTC),
                            brukernotifikasjonEntity.eventId().toString(),
                            brukernotifikasjonEntity.oppfolgingsPeriodeId().toString()
                    );
                    sendDone(brukernotifikasjonEntity.fnr(), doneInfo);
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

    @SneakyThrows
    public void sendDone(Fnr fnr, DoneInfo doneInfo) {
        NokkelInput nokkel = NokkelInput.newBuilder()
                .setAppnavn(applicationName)
                .setNamespace(namespace)
                .setFodselsnummer(fnr.get())
                .setGrupperingsId(doneInfo.getOppfolgingsperiode())
                .setEventId(doneInfo.getEventId())
                .build();

        LocalDateTime localUTCtime = doneInfo.avsluttetTidspunkt.toLocalDateTime().atZone(ZoneOffset.UTC).toLocalDateTime();

        // Tidspunkt skal ifølge doc være UTC
        DoneInput done = new DoneInputBuilder().withTidspunkt(localUTCtime).build();

        final ProducerRecord<NokkelInput, DoneInput> kafkaMelding = new ProducerRecord<>(doneTopic, nokkel, done);

        CompletableFuture<SendResult<NokkelInput, DoneInput>> future = kafkaDoneProducer.send(kafkaMelding);
        kafkaDoneProducer.flush();
        future.get();
        log.info("Sendt done for brukernotifikasjonsid: {}", doneInfo.getEventId());
    }

}
