package no.nav.fo.veilarbdialog.kvp;

import lombok.SneakyThrows;
import no.nav.common.json.JsonUtils;
import no.nav.fo.veilarbdialog.SpringBootTestBase;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.domain.AvsenderType;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.domain.kafka.KvpAvsluttetKafkaDTO;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.oppfolging.siste_periode.OppfolgingsperiodeV1;
import org.apache.kafka.clients.consumer.Consumer;
import org.assertj.core.api.Assertions;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.ZonedDateTime;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyHenvendelse;

class KvpConsumerTest extends SpringBootTestBase {

    private static final String AKTORID = "4321";
    private static final String SAKSBEHANDLER = "Z99999";
    private static final String BEGRUNNELSE = "Derfor";
    private static final String AKTOR_ID_1234 = "1234";
    private static final ZonedDateTime AVSLUTTETDATO = ZonedDateTime.now();
    @Autowired
    private DialogDAO dialogDAO;

    @Autowired
    KafkaTemplate<String, String> producer;
    Consumer<String, String> kvpAvsluttetConsumer;

    @Value("${application.topic.inn.oppfolgingsperiode}")
    String oppfolgingsperiodeTopic;

    @Value("${application.topic.inn.kvpavsluttet}")
    String kvpAvsluttetTopic;

    @BeforeEach
    void setupConsumer() {
        this.kvpAvsluttetConsumer = kafkaTestService.createStringStringConsumer(kvpAvsluttetTopic);
    }

    @SneakyThrows
    @Test
    void behandleKvpAvsluttetConsumerService_spiser_meldinger_fra_kvpAvsluttetTopic() {
        KvpAvsluttetKafkaDTO kvpAvsluttetKafkaDTO = avsluttKvpMelding(AKTORID);
        SendResult<String, String> sendResult = producer.send(kvpAvsluttetTopic, kvpAvsluttetKafkaDTO.getAktorId(), JsonUtils.toJson(kvpAvsluttetKafkaDTO)).get();

        kafkaTestService.assertErKonsumertAiven(kvpAvsluttetTopic, sendResult.getRecordMetadata().offset(), sendResult.getRecordMetadata().partition(), 10);
    }

    @SneakyThrows
    @Test
    void avslutte_kvp_bruker_som_ikke_er_under_kvp() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        String aktorId = mockBruker.getAktorId();
        UUID oppfolgingsId = mockBruker.getOppfolgingsperiode();
        OppfolgingsperiodeV1 startOppfolging = OppfolgingsperiodeV1.builder().uuid(oppfolgingsId).aktorId(aktorId).startDato(ZonedDateTime.now().minusDays(3).truncatedTo(MILLIS)).build();

        SendResult<String, String> sendResult = producer.send(oppfolgingsperiodeTopic, startOppfolging.getAktorId(), JsonUtils.toJson(startOppfolging)).get(1, SECONDS);
        kafkaTestService.assertErKonsumertAiven(oppfolgingsperiodeTopic, sendResult.getRecordMetadata().offset(), sendResult.getRecordMetadata().partition(), 10);

        DialogData dialog = nyDialog(AKTOR_ID_1234)
                .toBuilder()
                .overskrift("ny")
                .opprettetDato(DateTime.now().minusDays(2).toDate())
                .build();
        DialogData dialogData = dialogDAO.opprettDialog(dialog);
        lagHenvendelse(nyHenvendelse(dialogData.getId(), AKTOR_ID_1234, AvsenderType.BRUKER).withTekst("Hallo"));
        lagHenvendelse(nyHenvendelse(dialogData.getId(), AKTOR_ID_1234, AvsenderType.VEILEDER).withTekst("Hallo"));
        lagHenvendelse(nyHenvendelse(dialogData.getId(), AKTOR_ID_1234, AvsenderType.BRUKER).withTekst("Hvordan står det til?"));
        lagHenvendelse(nyHenvendelse(dialogData.getId(), AKTOR_ID_1234, AvsenderType.VEILEDER).withTekst("Bare bra"));
        lagHenvendelse(nyHenvendelse(dialogData.getId(), AKTOR_ID_1234, AvsenderType.BRUKER).withTekst("Hei på deg"));
        lagHenvendelse(nyHenvendelse(dialogData.getId(), AKTOR_ID_1234, AvsenderType.VEILEDER).withTekst("Hei på deg Ludvigsen"));

        KvpAvsluttetKafkaDTO kvpAvsluttetKafkaDTO = avsluttKvpMelding(AKTOR_ID_1234);
        SendResult<String, String> sendResultKvpAvsluttet = producer.send(kvpAvsluttetTopic, AKTOR_ID_1234, JsonUtils.toJson(kvpAvsluttetKafkaDTO)).get();
        kafkaTestService.assertErKonsumertAiven(kvpAvsluttetTopic, sendResultKvpAvsluttet.getRecordMetadata().offset(), sendResultKvpAvsluttet.getRecordMetadata().partition(), 10);

        Thread.sleep(2_000L);

        DialogData dialogEtter = dialogDAO.hentDialog(dialogData.getId());

        Assertions.assertThat(dialogEtter.isHistorisk()).isFalse();
    }

    private HenvendelseData lagHenvendelse(HenvendelseData dialogData) {
        return dialogDAO.opprettHenvendelse(
                dialogData.withSendt(DateTime.now().minusDays(2).toDate())
        );
    }

    @SneakyThrows
    @Test
    void avslutte_kvp_bruker_som_er_under_kvp() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        String aktorId = mockBruker.getAktorId();
        UUID oppfolgingsId = mockBruker.getOppfolgingsperiode();
        OppfolgingsperiodeV1 startOppfolging = OppfolgingsperiodeV1.builder().uuid(oppfolgingsId)
                .aktorId(aktorId).startDato(ZonedDateTime.now().minusDays(3).truncatedTo(MILLIS)).build();

        SendResult<String, String> sendResult = producer.send(oppfolgingsperiodeTopic, startOppfolging.getAktorId(), JsonUtils.toJson(startOppfolging)).get(1, SECONDS);
        kafkaTestService.assertErKonsumertAiven(oppfolgingsperiodeTopic, sendResult.getRecordMetadata().offset(), sendResult.getRecordMetadata().partition(), 10);

        DialogData dialog = nyDialog(AKTOR_ID_1234)
                .toBuilder()
                .overskrift("ny")
                .kontorsperreEnhetId("ett ord")
                .opprettetDato(DateTime.now().minusDays(2).toDate())
                .build();
        DialogData dialogData = dialogDAO.opprettDialog(dialog);
        lagHenvendelse(nyHenvendelse(dialogData.getId(), AKTOR_ID_1234, AvsenderType.BRUKER).withTekst("Hallo"));
        lagHenvendelse(nyHenvendelse(dialogData.getId(), AKTOR_ID_1234, AvsenderType.VEILEDER).withTekst("Hallo"));
        lagHenvendelse(nyHenvendelse(dialogData.getId(), AKTOR_ID_1234, AvsenderType.BRUKER).withTekst("Hvordan står det til?"));
        lagHenvendelse(nyHenvendelse(dialogData.getId(), AKTOR_ID_1234, AvsenderType.VEILEDER).withTekst("Bare bra").withKontorsperreEnhetId("nei to ord"));
        lagHenvendelse(nyHenvendelse(dialogData.getId(), AKTOR_ID_1234, AvsenderType.BRUKER).withTekst("Hei på deg").withKontorsperreEnhetId("jeg mener tre ord"));
        lagHenvendelse(nyHenvendelse(dialogData.getId(), AKTOR_ID_1234, AvsenderType.VEILEDER).withTekst("Hei på deg Ludvigsen").withKontorsperreEnhetId("dette er altså fire ord"));

        KvpAvsluttetKafkaDTO kvpAvsluttetKafkaDTO = avsluttKvpMelding(AKTOR_ID_1234);
        SendResult<String, String> sendResultKvpAvsluttet = producer.send(kvpAvsluttetTopic, AKTOR_ID_1234, JsonUtils.toJson(kvpAvsluttetKafkaDTO)).get();
        kafkaTestService.assertErKonsumertAiven(kvpAvsluttetTopic, sendResultKvpAvsluttet.getRecordMetadata().offset(), sendResultKvpAvsluttet.getRecordMetadata().partition(), 10);

        Thread.sleep(2_000L);

        DialogData dialogEtter = dialogDAO.hentDialog(dialogData.getId());

        Assertions.assertThat(dialogEtter.isHistorisk()).isTrue();
    }

    private KvpAvsluttetKafkaDTO avsluttKvpMelding(String aktorid) {
        return KvpAvsluttetKafkaDTO.builder()
                .aktorId(aktorid)
                .avsluttetAv(SAKSBEHANDLER)
                .avsluttetBegrunnelse(BEGRUNNELSE)
                .avsluttetDato(AVSLUTTETDATO)
                .build();
    }
}
