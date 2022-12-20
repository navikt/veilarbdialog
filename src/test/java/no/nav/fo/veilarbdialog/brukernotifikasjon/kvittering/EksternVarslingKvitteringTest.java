package no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.micrometer.core.instrument.MeterRegistry;
import io.restassured.RestAssured;
import lombok.SneakyThrows;
import no.nav.common.types.identer.Fnr;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonRepository;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType;
import no.nav.fo.veilarbdialog.brukernotifikasjon.VarselKvitteringStatus;
import no.nav.fo.veilarbdialog.brukernotifikasjon.entity.BrukernotifikasjonEntity;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.EskaleringsvarselDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import no.nav.fo.veilarbdialog.util.DialogTestService;
import no.nav.fo.veilarbdialog.util.KafkaTestService;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.assertj.core.api.SoftAssertions;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.concurrent.ListenableFuture;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering.EksternVarslingKvitteringConsumer.*;
import static org.junit.Assert.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
public class EksternVarslingKvitteringTest {

    @Autowired
    BrukernotifikasjonRepository brukernotifikasjonRepository;

    @Autowired
    KafkaTestService kafkaTestService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    DialogTestService dialogTestService;

    @Autowired
    NamedParameterJdbcTemplate jdbc;

    @Value("${application.topic.inn.eksternVarselKvittering}")
    String kvitteringsTopic;

    @Value("${spring.application.name}")
    String appname;

    @Autowired
    KafkaTemplate<String, DoknotifikasjonStatus> kvitteringsProducer;

    @Autowired
    MeterRegistry meterRegistry;

    @LocalServerPort
    private int port;

    private final static String OPPGAVE_KVITTERINGS_PREFIX = "O-veilarbdialog-";

    @Before
    public void setUp() {
        RestAssured.port = port;
    }

    @After
    public void assertNoUnkowns() {
        assertTrue(WireMock.findUnmatchedRequests().isEmpty());
    }

    @SneakyThrows
    @Test
    public void skal_oppdatere_brukernotifikasjon() {
        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);

        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(bruker.getFnr()), "begrunnelse", "overskrift", "henvendelseTekst");
        EskaleringsvarselDto startEskalering = dialogTestService.startEskalering(veileder, startEskaleringDto);

        BrukernotifikasjonEntity opprinneligBrukernotifikasjon = brukernotifikasjonRepository.hentBrukernotifikasjonForDialogId(startEskalering.tilhorendeDialogId(), BrukernotifikasjonsType.OPPGAVE).get(0);

        DoknotifikasjonStatus infoMelding = infoStatus(opprinneligBrukernotifikasjon.eventId());
        RecordMetadata infoRecordMetadata = sendKvitteringsMelding(infoMelding);
        assertExpectedBrukernotifikasjonStatus(startEskalering.tilhorendeDialogId(), opprinneligBrukernotifikasjon, infoRecordMetadata, VarselKvitteringStatus.IKKE_SATT);
        assertKvitteringLagret(opprinneligBrukernotifikasjon.eventId());

        DoknotifikasjonStatus oversendtMelding = oversendtStatus(opprinneligBrukernotifikasjon.eventId());
        RecordMetadata oversendtRecordMetadata = sendKvitteringsMelding(oversendtMelding);
        assertExpectedBrukernotifikasjonStatus(startEskalering.tilhorendeDialogId(), opprinneligBrukernotifikasjon, oversendtRecordMetadata, VarselKvitteringStatus.IKKE_SATT);

        DoknotifikasjonStatus ferdigstiltMelding = ferdigstiltStatus(opprinneligBrukernotifikasjon.eventId());
        RecordMetadata ferdigstiltRecordMetadata = sendKvitteringsMelding(ferdigstiltMelding);
        assertExpectedBrukernotifikasjonStatus(startEskalering.tilhorendeDialogId(), opprinneligBrukernotifikasjon, ferdigstiltRecordMetadata, VarselKvitteringStatus.OK);

        DoknotifikasjonStatus feiletMelding = feiletStatus(opprinneligBrukernotifikasjon.eventId());
        RecordMetadata feiletRecordMetadata = sendKvitteringsMelding(feiletMelding);
        assertExpectedBrukernotifikasjonStatus(startEskalering.tilhorendeDialogId(), opprinneligBrukernotifikasjon, feiletRecordMetadata, VarselKvitteringStatus.FEILET);

    }
    
    private RecordMetadata sendKvitteringsMelding(DoknotifikasjonStatus melding) throws ExecutionException, InterruptedException {
        ListenableFuture<SendResult<String, DoknotifikasjonStatus>> send = kvitteringsProducer.send(kvitteringsTopic, melding);
        kvitteringsProducer.flush();

        return send.get().getRecordMetadata();
    }

    private void assertExpectedBrukernotifikasjonStatus(Long dialogId, BrukernotifikasjonEntity opprinneligBrukernotifikasjon, RecordMetadata recordMetadata, VarselKvitteringStatus expectedStatus) {
        long offset = recordMetadata.offset();
        int partition = recordMetadata.partition();

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> kafkaTestService.erKonsumert(kvitteringsTopic, "veilarbdialog", offset, partition));

        BrukernotifikasjonEntity brukernotifikasjonEtterProsessering = brukernotifikasjonRepository.hentBrukernotifikasjonForDialogId(dialogId, BrukernotifikasjonsType.OPPGAVE).get(0);

        SoftAssertions.assertSoftly( assertions -> {
            assertions.assertThat(brukernotifikasjonEtterProsessering.eventId()).isEqualTo(opprinneligBrukernotifikasjon.eventId());
            assertions.assertThat(brukernotifikasjonEtterProsessering.varselKvitteringStatus()).isEqualTo(expectedStatus);
            assertions.assertAll();
        });
    }


    private DoknotifikasjonStatus lagDoknotifikasjonStatusMelding(UUID eventId, String status) {
        String bestillingsId = eventId.toString();
        return DoknotifikasjonStatus
                .newBuilder()
                .setStatus(status)
                .setBestillingsId(bestillingsId)
                .setBestillerId(appname)
                .setMelding("her er en melding")
                .setDistribusjonId(1L)
                .build();
    }

    private void assertKvitteringLagret(UUID bestillingsId) {
        Awaitility.await().atMost(Duration.of(10, ChronoUnit.SECONDS)).until( () -> {
            String status = null;
            SqlParameterSource params = new MapSqlParameterSource()
                    .addValue("bestillingId", bestillingsId.toString());
            List<String> list = jdbc.queryForList("""
                    SELECT DOKNOTIFIKASJON_STATUS
                    FROM EKSTERN_VARSEL_KVITTERING
                    WHERE BRUKERNOTIFIKASJON_BESTILLING_ID = :bestillingId
                    """, params, String.class);

            return list.size() > 0;
        });
    }

    private DoknotifikasjonStatus ferdigstiltStatus(UUID bestillingsId) {
        return lagDoknotifikasjonStatusMelding(bestillingsId, FERDIGSTILT);
    }

    private DoknotifikasjonStatus feiletStatus(UUID bestillingsId) {
        return lagDoknotifikasjonStatusMelding(bestillingsId, FEILET);
    }

    private DoknotifikasjonStatus infoStatus(UUID bestillingsId) {
        return lagDoknotifikasjonStatusMelding(bestillingsId, INFO);
    }

    private DoknotifikasjonStatus oversendtStatus(UUID eventId) {
        return lagDoknotifikasjonStatusMelding(eventId, OVERSENDT);
    }

}