package no.nav.fo.veilarbdialog.rest;

import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.fo.veilarbdialog.brukernotifikasjon.*;
import no.nav.fo.veilarbdialog.brukernotifikasjon.entity.BrukernotifikasjonEntity;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.mock_nav_modell.BrukerOptions;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import no.nav.fo.veilarbdialog.service.ScheduleRessurs;
import no.nav.fo.veilarbdialog.util.KafkaTestService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.SoftAssertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
@Slf4j
@Sql(
        scripts = "/db/testdata/slett_alle_dialoger.sql",
        executionPhase = BEFORE_TEST_METHOD
)
public class DialogBeskjedTest {

    @LocalServerPort
    protected int port;

    @Autowired
    ScheduleRessurs scheduleRessurs;

    @Autowired
    BrukernotifikasjonRepository brukernotifikasjonRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    LockProvider lockProvider;

    @Autowired
    KafkaTestService kafkaTestService;

    @Autowired
    BrukernotifikasjonService brukernotifikasjonService;

    @Value("${application.topic.ut.brukernotifikasjon.beskjed}")
    private String brukernotifikasjonBeskjedTopic;

    @Value("${application.topic.ut.brukernotifikasjon.done}")
    private String brukernotifikasjonDoneTopic;

    Consumer<NokkelInput, BeskjedInput> brukerNotifikasjonBeskjedConsumer;

    Consumer<NokkelInput, DoneInput> brukerNotifikasjonDoneConsumer;

    @Before
    public void setup() {
        JdbcTemplateLockProvider l = (JdbcTemplateLockProvider) lockProvider;
        l.clearCache();
        RestAssured.port = port;
        brukerNotifikasjonBeskjedConsumer = kafkaTestService.createAvroAvroConsumer(brukernotifikasjonBeskjedTopic);
        brukerNotifikasjonDoneConsumer = kafkaTestService.createAvroAvroConsumer(brukernotifikasjonDoneTopic);
    }

    @Test
    public void beskjed_happy_case() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);

        DialogDTO dialog = mockVeileder.createRequest()
                .body(new NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift"))
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", mockBruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        // Setter sendt til å være 1 sekund tidligere pga. grace period
        settHenvendelseSendtForNSekundSiden(dialog.getHenvendelser().get(0).getId(), 1);

        scheduleRessurs.sendBrukernotifikasjonerForUlesteDialoger();
        brukernotifikasjonService.sendPendingBrukernotifikasjoner();

        ConsumerRecord<NokkelInput, BeskjedInput> brukernotifikasjonRecord =
                KafkaTestUtils.getSingleRecord(brukerNotifikasjonBeskjedConsumer, brukernotifikasjonBeskjedTopic, 5000L);

        assertThat(brukernotifikasjonRecord.value().getTekst()).isEqualTo(BrukernotifikasjonTekst.BESKJED_BRUKERNOTIFIKASJON_TEKST);

        BrukernotifikasjonEntity brukernotifikasjonEntity =
                brukernotifikasjonRepository.hentBrukernotifikasjonForDialogId(Long.parseLong(dialog.getId()), BrukernotifikasjonsType.BESKJED).get(0);

        SoftAssertions.assertSoftly(
                assertions -> {
                    assertions.assertThat(brukernotifikasjonEntity.dialogId()).isEqualTo(Long.valueOf(dialog.getId()));
                    assertions.assertThat(brukernotifikasjonEntity.type()).isEqualTo(BrukernotifikasjonsType.BESKJED);
                    assertions.assertThat(brukernotifikasjonEntity.status()).isEqualTo(BrukernotifikasjonBehandlingStatus.SENDT);
                }
        );

        mockBruker.createRequest()
                .put("/veilarbdialog/api/dialog/{dialogId}/les", dialog.getId())
                .then()
                .statusCode(200);

        brukernotifikasjonService.sendDoneBrukernotifikasjoner();

        ConsumerRecord<NokkelInput, DoneInput> doneRecord =
                KafkaTestUtils.getSingleRecord(brukerNotifikasjonDoneConsumer, brukernotifikasjonDoneTopic, 5000L);

        NokkelInput nokkel = doneRecord.key();

        assertThat(mockBruker.getFnr()).isEqualTo(nokkel.getFodselsnummer());
    }

    @Test
    public void ikke_beskjed_foer_grace_periode_utlopt() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);

        mockVeileder.createRequest()
                .body(new NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift"))
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", mockBruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        scheduleRessurs.sendBrukernotifikasjonerForUlesteDialoger();
        brukernotifikasjonService.sendPendingBrukernotifikasjoner();

        boolean harKonsumertAlleMeldinger = kafkaTestService.harKonsumertAlleMeldinger(brukernotifikasjonBeskjedTopic, brukerNotifikasjonBeskjedConsumer);
        assertThat(harKonsumertAlleMeldinger).isTrue();
    }

    @Test
    public void ikke_beskjed_naar_maxalder_er_passert() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);

        DialogDTO dialog = mockVeileder.createRequest()
                .body(new NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift"))
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", mockBruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        // Setter sendt til å være 3 sekund tidligere pga. max alder
        settHenvendelseSendtForNSekundSiden(dialog.getHenvendelser().get(0).getId(), 3);

        scheduleRessurs.sendBrukernotifikasjonerForUlesteDialoger();
        brukernotifikasjonService.sendPendingBrukernotifikasjoner();

        boolean harKonsumertAlleMeldinger = kafkaTestService.harKonsumertAlleMeldinger(brukernotifikasjonBeskjedTopic, brukerNotifikasjonBeskjedConsumer);
        assertThat(harKonsumertAlleMeldinger).isTrue();
    }

    @Test
    public void kan_ikke_varsles() {
        MockBruker bruker = MockNavService.createHappyBruker();
        BrukerOptions reservertKrr = bruker.getBrukerOptions().toBuilder().erReservertKrr(true).build();
        MockNavService.updateBruker(bruker, reservertKrr);

        MockVeileder veileder = MockNavService.createVeileder(bruker);

        veileder.createRequest()
                .body(new NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift"))
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", bruker.getAktorId())
                .then()
                .statusCode(409);

    }

    @Test
    public void ikkeDobleNotifikasjonerPaaSammeDialog() {

        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);

        DialogDTO dialog = mockVeileder.createRequest()
                .body(new NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift"))
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", mockBruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        // Hy henvendelse samme dialog
        DialogDTO sammeDialog = mockVeileder.createRequest()
                .body(new NyHenvendelseDTO().setTekst("tekst2").setDialogId(dialog.getId()))
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", mockBruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        // Setter sendt til å være 1 sekund tidligere pga. grace period
        settHenvendelseSendtForNSekundSiden(sammeDialog.getHenvendelser().get(0).getId(), 1);
        settHenvendelseSendtForNSekundSiden(sammeDialog.getHenvendelser().get(1).getId(), 1);

        scheduleRessurs.sendBrukernotifikasjonerForUlesteDialoger();
        brukernotifikasjonService.sendPendingBrukernotifikasjoner();

        ConsumerRecord<NokkelInput, BeskjedInput> brukernotifikasjonRecord =
                KafkaTestUtils.getSingleRecord(brukerNotifikasjonBeskjedConsumer, brukernotifikasjonBeskjedTopic, 20000L);

        Assert.assertTrue(kafkaTestService.harKonsumertAlleMeldinger(brukernotifikasjonBeskjedTopic, brukerNotifikasjonBeskjedConsumer));

        assertThat(brukernotifikasjonRecord.value().getTekst()).isEqualTo(BrukernotifikasjonTekst.BESKJED_BRUKERNOTIFIKASJON_TEKST);




        BrukernotifikasjonEntity brukernotifikasjonEntity =
                brukernotifikasjonRepository.hentBrukernotifikasjonForDialogId(Long.parseLong(dialog.getId()), BrukernotifikasjonsType.BESKJED).get(0);

        SoftAssertions.assertSoftly(
                assertions -> {
                    assertions.assertThat(brukernotifikasjonEntity.dialogId()).isEqualTo(Long.valueOf(dialog.getId()));
                    assertions.assertThat(brukernotifikasjonEntity.type()).isEqualTo(BrukernotifikasjonsType.BESKJED);
                    assertions.assertThat(brukernotifikasjonEntity.status()).isEqualTo(BrukernotifikasjonBehandlingStatus.SENDT);
                }
        );

        mockBruker.createRequest()
                .put("/veilarbdialog/api/dialog/{dialogId}/les", dialog.getId())
                .then()
                .statusCode(200);

        brukernotifikasjonService.sendDoneBrukernotifikasjoner();

        ConsumerRecord<NokkelInput, DoneInput> doneRecord =
                KafkaTestUtils.getSingleRecord(brukerNotifikasjonDoneConsumer, brukernotifikasjonDoneTopic, 10000L);

        Assert.assertTrue(kafkaTestService.harKonsumertAlleMeldinger(brukernotifikasjonDoneTopic, brukerNotifikasjonDoneConsumer));

        NokkelInput nokkel = doneRecord.key();

        assertThat(mockBruker.getFnr()).isEqualTo(nokkel.getFodselsnummer());

    }

    @Test
    public void ingenDobleNotifikasjonerPaaSammeDialogMedIntervall() {

        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);

        DialogDTO dialog = mockVeileder.createRequest()
                .body(new NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift"))
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", mockBruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);



        // Setter sendt til å være 1 sekund tidligere pga. grace period
        settHenvendelseSendtForNSekundSiden(dialog.getHenvendelser().get(0).getId(), 1);

        scheduleRessurs.sendBrukernotifikasjonerForUlesteDialoger();
        brukernotifikasjonService.sendPendingBrukernotifikasjoner();

        ConsumerRecord<NokkelInput, BeskjedInput> brukernotifikasjonRecord =
                KafkaTestUtils.getSingleRecord(brukerNotifikasjonBeskjedConsumer, brukernotifikasjonBeskjedTopic, 20000L);

        Assert.assertTrue(kafkaTestService.harKonsumertAlleMeldinger(brukernotifikasjonBeskjedTopic, brukerNotifikasjonBeskjedConsumer));

        assertThat(brukernotifikasjonRecord.value().getTekst()).isEqualTo(BrukernotifikasjonTekst.BESKJED_BRUKERNOTIFIKASJON_TEKST);


        // Hy henvendelse samme dialog
        DialogDTO sammedialog = mockVeileder.createRequest()
                .body(new NyHenvendelseDTO().setTekst("tekst2").setDialogId(dialog.getId()))
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", mockBruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);


        // Setter sendt til å være 1 sekund tidligere pga. grace period
        settHenvendelseSendtForNSekundSiden(sammedialog.getHenvendelser().get(1).getId(), 1);

        scheduleRessurs.sendBrukernotifikasjonerForUlesteDialoger();
        brukernotifikasjonService.sendPendingBrukernotifikasjoner();

        // Ingen nye beskjeder siden forrige henvendelse ikke er lest

        Assert.assertTrue(kafkaTestService.harKonsumertAlleMeldinger(brukernotifikasjonBeskjedTopic, brukerNotifikasjonBeskjedConsumer));

        mockBruker.createRequest()
                .put("/veilarbdialog/api/dialog/{dialogId}/les", dialog.getId())
                .then()
                .statusCode(200);

        brukernotifikasjonService.sendDoneBrukernotifikasjoner();

        ConsumerRecord<NokkelInput, DoneInput> doneRecord =
                KafkaTestUtils.getSingleRecord(brukerNotifikasjonDoneConsumer, brukernotifikasjonDoneTopic, 10000L);

        Assert.assertTrue(kafkaTestService.harKonsumertAlleMeldinger(brukernotifikasjonDoneTopic, brukerNotifikasjonDoneConsumer));

        NokkelInput nokkel = doneRecord.key();

        assertThat(mockBruker.getFnr()).isEqualTo(nokkel.getFodselsnummer());

    }


    @Test
    public void nyNotifikasjonPaaSammeDialogNaarFoersteErLest() throws InterruptedException {

        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);

        DialogDTO dialog = mockVeileder.createRequest()
                .body(new NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift"))
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", mockBruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        settHenvendelseSendtForNSekundSiden(dialog.getHenvendelser().get(0).getId(), 1);
        scheduleRessurs.sendBrukernotifikasjonerForUlesteDialoger();
        brukernotifikasjonService.sendPendingBrukernotifikasjoner();


        ConsumerRecord<NokkelInput, BeskjedInput> brukernotifikasjonRecord =
                KafkaTestUtils.getSingleRecord(brukerNotifikasjonBeskjedConsumer, brukernotifikasjonBeskjedTopic, 10000L);

        Assert.assertTrue(kafkaTestService.harKonsumertAlleMeldinger(brukernotifikasjonBeskjedTopic, brukerNotifikasjonBeskjedConsumer));

        assertThat(brukernotifikasjonRecord.value().getTekst()).isEqualTo(BrukernotifikasjonTekst.BESKJED_BRUKERNOTIFIKASJON_TEKST);

        mockBruker.createRequest()
                .put("/veilarbdialog/api/dialog/{dialogId}/les", dialog.getId())
                .then()
                .statusCode(200);

        brukernotifikasjonService.sendDoneBrukernotifikasjoner();

        ConsumerRecord<NokkelInput, DoneInput> doneRecord =
                KafkaTestUtils.getSingleRecord(brukerNotifikasjonDoneConsumer, brukernotifikasjonDoneTopic, 10000L);

        Assert.assertTrue(kafkaTestService.harKonsumertAlleMeldinger(brukernotifikasjonDoneTopic, brukerNotifikasjonDoneConsumer));



        // Hy henvendelse samme dialog
        DialogDTO sammeDialog = mockVeileder.createRequest()
                .body(new NyHenvendelseDTO().setTekst("tekst2").setDialogId(dialog.getId()))
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", mockBruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        Thread.sleep(1000L);
        scheduleRessurs.sendBrukernotifikasjonerForUlesteDialoger();
        brukernotifikasjonService.sendPendingBrukernotifikasjoner();

        // Ny beskjed siden forrige henvendelse er lest

        Assert.assertFalse(kafkaTestService.harKonsumertAlleMeldinger(brukernotifikasjonBeskjedTopic, brukerNotifikasjonBeskjedConsumer));


    }

    // Setter sendt til å være N sekund tidligere pga. grace period
    private void settHenvendelseSendtForNSekundSiden(String henvendelseId, int sekunderSiden) {
        Date nySendt = Date.from(Instant.now().minus(sekunderSiden, ChronoUnit.SECONDS));
        int update = jdbcTemplate.update("""
                                UPDATE HENVENDELSE
                                SET SENDT = ?
                                WHERE HENVENDELSE_ID = ?
                        """,
                nySendt,
                henvendelseId
        );
        assertThat(update).isEqualTo(1);
    }

}
