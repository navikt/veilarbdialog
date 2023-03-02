package no.nav.fo.veilarbdialog.brukernotifikasjon;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.brukernotifikasjon.schemas.builders.BeskjedInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.DoneInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal;
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
import no.nav.fo.veilarbdialog.clients.veilarbperson.Nivaa4DTO;
import no.nav.fo.veilarbdialog.clients.veilarbperson.VeilarbpersonClient;
import no.nav.fo.veilarbdialog.db.dao.VarselDAO;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerKanIkkeVarslesException;
import no.nav.fo.veilarbdialog.oppfolging.v2.OppfolgingV2Client;
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

    private final VeilarbpersonClient veilarbpersonClient;

    private final BrukernotifikasjonRepository brukernotifikasjonRepository;

    private final KafkaTemplate<NokkelInput, OppgaveInput> kafkaOppgaveProducer;

    private final KafkaTemplate<NokkelInput, BeskjedInput> kafkaBeskjedProducer;

    private final KafkaTemplate<NokkelInput, DoneInput> kafkaDoneProducer;

    private final VarselDAO varselDAO;

    private final KvitteringDAO kvitteringDAO;

    private final KvitteringMetrikk kvitteringMetrikk;

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
        pendingBrukernotifikasjoner.stream().forEach(
                brukernotifikasjonEntity ->  {
                    switch (brukernotifikasjonEntity.type()) {
                        case OPPGAVE -> {
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
                        }
                        case BESKJED -> {
                            BeskjedInfo beskjedInfo = new BeskjedInfo(
                                    brukernotifikasjonEntity.eventId().toString(),
                                    brukernotifikasjonEntity.melding(),
                                    brukernotifikasjonEntity.oppfolgingsPeriodeId().toString(),
                                    brukernotifikasjonEntity.epostTittel(),
                                    brukernotifikasjonEntity.epostBody(),
                                    brukernotifikasjonEntity.smsText(),
                                    brukernotifikasjonEntity.lenke()
                            );
                            sendBeskjed(brukernotifikasjonEntity.fnr(), beskjedInfo);
                        }
                    }
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
        Optional<Nivaa4DTO> nivaa4DTO = veilarbpersonClient.hentNiva4(fnr);

        boolean erManuell = manuellStatusResponse.map(ManuellStatusV2DTO::isErUnderManuellOppfolging).orElse(true);
        boolean erReservertIKrr = manuellStatusResponse.map(ManuellStatusV2DTO::getKrrStatus).map(ManuellStatusV2DTO.KrrStatus::isErReservert).orElse(true);
        boolean harBruktNivaa4 = nivaa4DTO.map(Nivaa4DTO::isHarbruktnivaa4).orElse(false);

        boolean kanVarsles = !erManuell && !erReservertIKrr && harBruktNivaa4;

        if(!kanVarsles) {
            secureLogs.warn("bruker med fnr: {} kan ikke varsles, statuser erManuell: {}, erReservertIKrr: {}, harBruktNivaa4: {}", fnr, erManuell, erReservertIKrr, harBruktNivaa4);
        }

        return kanVarsles;
    }

    @SneakyThrows
    private void sendBeskjed(Fnr fnr, BeskjedInfo beskjedInfo) {
        NokkelInput nokkel = new NokkelInputBuilder()
                .withFodselsnummer(fnr.get())
                .withEventId(beskjedInfo.getBrukernotifikasjonId())
                .withAppnavn(applicationName)
                .withNamespace(namespace)
                .withGrupperingsId(beskjedInfo.getOppfolgingsperiode())
                .build();

        BeskjedInput beskjed = new BeskjedInputBuilder()
                .withTidspunkt(LocalDateTime.now(ZoneOffset.UTC))
                .withTekst(beskjedInfo.getMelding())
                .withLink(beskjedInfo.getLink())
                .withSikkerhetsnivaa(BESKJED_SIKKERHETSNIVAA)
                .withEksternVarsling(true)
                .withPrefererteKanaler(PreferertKanal.SMS)
                .withSmsVarslingstekst(beskjedInfo.getSmsTekst())
                .withEpostVarslingstittel(beskjedInfo.getEpostTitel())
                .withEpostVarslingstekst(beskjedInfo.getEpostBody())
                .withSynligFremTil(LocalDateTime.now(ZoneOffset.UTC).plusMonths(1))
                .build();

        final ProducerRecord<NokkelInput, BeskjedInput> kafkaMelding = new ProducerRecord<>(beskjedTopic, nokkel, beskjed);

        CompletableFuture<SendResult<NokkelInput, BeskjedInput>> future = kafkaBeskjedProducer.send(kafkaMelding);
        kafkaDoneProducer.flush();
        future.get();
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
                .withSynligFremTil(LocalDateTime.now(ZoneOffset.UTC).plusMonths(1))
                .build();

        final ProducerRecord<NokkelInput, OppgaveInput> kafkaMelding = new ProducerRecord<>(oppgaveTopic, nokkel, oppgave);

        CompletableFuture<SendResult<NokkelInput, OppgaveInput>> future = kafkaOppgaveProducer.send(kafkaMelding);
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
