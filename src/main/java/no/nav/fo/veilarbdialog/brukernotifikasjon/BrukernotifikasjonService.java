package no.nav.fo.veilarbdialog.brukernotifikasjon;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.brukernotifikasjon.schemas.builders.DoneInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
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
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BrukernotifikasjonService {

    private static final int OPPGAVE_SIKKERHETSNIVAA = 3;

    private final VeilarboppfolgingClient veilarboppfolgingClient;

    private final VeilarbpersonClient veilarbpersonClient;

    private final BrukernotifikasjonRepository brukernotifikasjonRepository;

    private final KafkaTemplate<NokkelInput, OppgaveInput> kafkaOppgaveProducer;

    private final KafkaTemplate<NokkelInput, DoneInput> kafkaDoneProducer;

    @Value("${application.topic.ut.brukernotifikasjon.oppgave}")
    private String oppgaveTopic;

    @Value("${application.topic.ut.brukernotifikasjon.done}")
    private String doneTopic;

    @Value("${spring.application.name}")
    String applicationName;

    @Value("${application.namespace}")
    String namespace;

    public BrukernotifikasjonEntity sendBrukernotifikasjon(Brukernotifikasjon brukernotifikasjon) {
        OppgaveInfo oppgaveInfo = new OppgaveInfo(
                brukernotifikasjon.eventId().toString(),
                brukernotifikasjon.melding(),
                brukernotifikasjon.oppfolgingsperiodeId().toString(),
                brukernotifikasjon.epostTitel(),
                brukernotifikasjon.epostBody(),
                brukernotifikasjon.smsTekst(),
                brukernotifikasjon.link()
        );

        sendOppgave(brukernotifikasjon.foedselsnummer(), oppgaveInfo);

        BrukernotifikasjonInsert insert = new BrukernotifikasjonInsert(
                brukernotifikasjon.eventId(),
                brukernotifikasjon.dialogId(),
                brukernotifikasjon.foedselsnummer(),
                brukernotifikasjon.melding(),
                brukernotifikasjon.oppfolgingsperiodeId(),
                brukernotifikasjon.type(),
                VarselStatus.SENDT,
                brukernotifikasjon.epostTitel(),
                brukernotifikasjon.epostBody(),
                brukernotifikasjon.smsTekst(),
                brukernotifikasjon.link()
        );

        Long id = brukernotifikasjonRepository.opprettBrukernotifikasjon(insert);
        return hentBrukernotifikasjon(id);
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

        // Tidspunkt skal ifølge doc være UTC
        DoneInput done = new DoneInputBuilder().withTidspunkt(LocalDateTime.now(ZoneOffset.UTC)).build();

        final ProducerRecord<NokkelInput, DoneInput> kafkaMelding = new ProducerRecord<>(doneTopic, nokkel, done);

        ListenableFuture<SendResult<NokkelInput, DoneInput>> future = kafkaDoneProducer.send(kafkaMelding);
        kafkaDoneProducer.flush();
        future.get();
    }

}
