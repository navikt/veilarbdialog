package no.nav.fo.veilarbdialog.brukernotifikasjon.oppgave;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.common.types.identer.Fnr;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@RequiredArgsConstructor
@Service
public class OppgaveProducer {
    private final KafkaTemplate<NokkelInput, OppgaveInput> kafkaOppgaveProducer;
    @Value("${application.topic.ut.brukernotifikasjon.oppgave}")
    private String oppgaveToppic;

    @SneakyThrows
    long sendOppgave(OppgaveInfo oppgaveInfo, Fnr fnr, URL aktivitetsLink) {
        int sikkerhetsnivaa = 3;

        NokkelInput nokkel = new NokkelInputBuilder()
                .withFodselsnummer(fnr.get())
                .withEventId(oppgaveInfo.getBrukernotifikasjonId())
                .withAppnavn("veilarbdialog")
                .withNamespace("pto")
                .withGrupperingsId(oppgaveInfo.getOppfolgingsperiode())
                .build();

        OppgaveInput oppgave = new OppgaveInputBuilder()
                .withTidspunkt(LocalDateTime.now(ZoneOffset.UTC))
                .withTekst(oppgaveInfo.getMelding())
                .withLink(aktivitetsLink)
                .withSikkerhetsnivaa(sikkerhetsnivaa)
                .withEksternVarsling(true)
                .withSmsVarslingstekst(oppgaveInfo.getSmsTekst())
                .withPrefererteKanaler(PreferertKanal.SMS)
                .withSmsVarslingstekst(oppgaveInfo.getSmsTekst())
                .withEpostVarslingstittel(oppgaveInfo.getEpostTitel())
                .withEpostVarslingstekst(oppgaveInfo.getEpostBody())
                .build();

        final ProducerRecord<NokkelInput, OppgaveInput> kafkaMelding = new ProducerRecord<>(oppgaveToppic, nokkel, oppgave);
        return kafkaOppgaveProducer.send(kafkaMelding).get().getRecordMetadata().offset();
    }

}
