package no.nav.fo.veilarbdialog.brukernotifikasjon;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.brukernotifikasjon.schemas.builders.DoneInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.brukernotifikasjon.entity.BrukernotifikasjonEntity;
import no.nav.fo.veilarbdialog.clients.veilarboppfolging.ManuellStatusV2DTO;
import no.nav.fo.veilarbdialog.clients.veilarboppfolging.VeilarboppfolgingClient;
import no.nav.fo.veilarbdialog.clients.veilarbperson.Nivaa4DTO;
import no.nav.fo.veilarbdialog.clients.veilarbperson.VeilarbpersonClient;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BrukernotifikasjonService {

    private static final int OPPGAVE_SIKKERHETSNIVAA = 3;

    private final VeilarboppfolgingClient veilarboppfolgingClient;

    private final VeilarbpersonClient veilarbpersonClient;

    private final BrukernotifikasjonRepository brukernotifikasjonRepository;

    private final KafkaTemplate<NokkelInput, OppgaveInput> kafkaOppgaveProducer;

    private final KafkaTemplate<NokkelInput, DoneInput> kafkaDoneProducer;

    private final LeaderElectionClient leaderElectionClient;

    @Value("${application.topic.ut.brukernotifikasjon.oppgave}")
    private String oppgaveTopic;

    @Value("${application.topic.ut.brukernotifikasjon.done}")
    private String doneTopic;

    @Value("${spring.application.name}")
    String applicationName;

    @Value("${application.namespace}")
    String namespace;

    public BrukernotifikasjonEntity bestillBrukernotifikasjon(Brukernotifikasjon brukernotifikasjon) {
        BrukernotifikasjonInsert insert = new BrukernotifikasjonInsert(
                brukernotifikasjon.eventId(),
                brukernotifikasjon.dialogId(),
                brukernotifikasjon.foedselsnummer(),
                brukernotifikasjon.melding(),
                brukernotifikasjon.oppfolgingsperiodeId(),
                brukernotifikasjon.type(),
                BrukernotifikasjonBehandlingStatus.PENDING,
                brukernotifikasjon.epostTitel(),
                brukernotifikasjon.epostBody(),
                brukernotifikasjon.smsTekst(),
                brukernotifikasjon.link()
        );

        Long id = brukernotifikasjonRepository.opprettBrukernotifikasjon(insert);
        return hentBrukernotifikasjon(id);
    }

    public void bestillDone(long brukernotifikasjonId) {
        brukernotifikasjonRepository.updateStatus(brukernotifikasjonId, BrukernotifikasjonBehandlingStatus.SKAL_AVSLUTTES);
    }

    @Scheduled(
            initialDelay = 60000,
            fixedDelay = 5000
    )
    @SchedulerLock(name = "brukernotifikasjon_oppgave_kafka_scheduledTask", lockAtMostFor = "PT2M")
    public void sendPendingBrukernotifikasjoner() {
        List<BrukernotifikasjonEntity> pendingBrukernotifikasjoner = brukernotifikasjonRepository.hentPendingBrukernotifikasjoner();
        pendingBrukernotifikasjoner.stream().forEach(
                brukernotifikasjonEntity ->  {
                    OppgaveInfo oppgaveInfo = new OppgaveInfo(
                            brukernotifikasjonEntity.eventId().toString(),
                            brukernotifikasjonEntity.melding(),
                            brukernotifikasjonEntity.oppfolgingsPeriodeId().toString(),
                            brukernotifikasjonEntity.epostTittel(),
                            brukernotifikasjonEntity.epostBody(),
                            brukernotifikasjonEntity.smsText(),
                            brukernotifikasjonEntity.lenke()
                    );
                    sendOppgave(brukernotifikasjonEntity.fnr(), oppgaveInfo);
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

    public boolean kanVarsles(Fnr fnr) {
        Optional<ManuellStatusV2DTO> manuellStatusResponse = veilarboppfolgingClient.hentManuellStatus(fnr);
        Optional<Nivaa4DTO> nivaa4DTO = veilarbpersonClient.hentNiva4(fnr);

        boolean erManuell = manuellStatusResponse.map(ManuellStatusV2DTO::isErUnderManuellOppfolging).orElse(true);
        boolean erReservertIKrr = manuellStatusResponse.map(ManuellStatusV2DTO::getKrrStatus).map(ManuellStatusV2DTO.KrrStatus::isErReservert).orElse(true);
        boolean harBruktNivaa4 = nivaa4DTO.map(Nivaa4DTO::isHarbruktnivaa4).orElse(false);

        return !erManuell && !erReservertIKrr && harBruktNivaa4;
    }

    @SneakyThrows
    private void sendOppgave(Fnr fnr, OppgaveInfo oppgaveInfo) {
        NokkelInput nokkel = new NokkelInputBuilder()
                .withFodselsnummer(fnr.get())
                .withEventId(oppgaveInfo.getBrukernotifikasjonId())
                .withAppnavn(applicationName)
                .withNamespace(namespace)
                .withGrupperingsId(oppgaveInfo.getOppfolgingsperiode())
                .build();

        OppgaveInput oppgave = new OppgaveInputBuilder()
                .withTidspunkt(LocalDateTime.now(ZoneOffset.UTC))
                .withTekst(oppgaveInfo.getMelding())
                .withLink(oppgaveInfo.getLink())
                .withSikkerhetsnivaa(OPPGAVE_SIKKERHETSNIVAA)
                .withEksternVarsling(true)
                .withSmsVarslingstekst(oppgaveInfo.getSmsTekst())
                .withPrefererteKanaler(PreferertKanal.SMS)
                .withSmsVarslingstekst(oppgaveInfo.getSmsTekst())
                .withEpostVarslingstittel(oppgaveInfo.getEpostTitel())
                .withEpostVarslingstekst(oppgaveInfo.getEpostBody())
                .build();

        final ProducerRecord<NokkelInput, OppgaveInput> kafkaMelding = new ProducerRecord<>(oppgaveTopic, nokkel, oppgave);

        ListenableFuture<SendResult<NokkelInput, OppgaveInput>> future = kafkaOppgaveProducer.send(kafkaMelding);
        kafkaDoneProducer.flush();
        future.get();
        log.info("Sendt oppgave med brukernotifikasjonsid: {}", oppgaveInfo.getBrukernotifikasjonId());
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

        // TODO set tidspunkt på DoneInput til denne?
        LocalDateTime localUTCtime = doneInfo.avsluttetTidspunkt.toLocalDateTime().atZone(ZoneOffset.UTC).toLocalDateTime();

        // Tidspunkt skal ifølge doc være UTC
        DoneInput done = new DoneInputBuilder().withTidspunkt(localUTCtime).build();

        final ProducerRecord<NokkelInput, DoneInput> kafkaMelding = new ProducerRecord<>(doneTopic, nokkel, done);

        ListenableFuture<SendResult<NokkelInput, DoneInput>> future = kafkaDoneProducer.send(kafkaMelding);
        kafkaDoneProducer.flush();
        future.get();
        log.info("Sendt done for brukernotifikasjonsid: {}", doneInfo.getEventId());
    }

}
