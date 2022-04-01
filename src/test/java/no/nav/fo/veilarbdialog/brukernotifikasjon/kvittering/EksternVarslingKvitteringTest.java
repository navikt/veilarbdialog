package no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.fo.veilarbdialog.util.KafkaTestService;
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
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.concurrent.ListenableFuture;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering.EksternVarslingKvitteringConsumer.OVERSENDT;
import static org.junit.Assert.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
public class EksternVarslingKvitteringTest {




    @Autowired
    KafkaTestService kafkaTestService;

    @Autowired
    JdbcTemplate jdbcTemplate;



    @Autowired
    NamedParameterJdbcTemplate jdbc;

    @Value("${application.topic.inn.eksternVarselKvittering}")
    String kvitteringsTopic;

    @Autowired
    KafkaTemplate<String, DoknotifikasjonStatus> kvitteringsProducer;



    @Autowired
    MeterRegistry meterRegistry;

    @LocalServerPort
    private int port;

    private final static String BESKJED_KVITTERINGS_PREFIX = "B-veilarbdialog-";

    @Before
    public void setUp() {
    }

    @After
    public void assertNoUnkowns() {

        assertTrue(WireMock.findUnmatchedRequests().isEmpty());
    }

    @SneakyThrows
    @Test
    public void skal_lagre_kvittering() {


        String brukernotifikasjonId = BESKJED_KVITTERINGS_PREFIX + UUID.randomUUID();


        DoknotifikasjonStatus melding = DoknotifikasjonStatus
                .newBuilder()
                .setStatus(OVERSENDT)
                .setBestillingsId(brukernotifikasjonId)
                .setBestillerId("veilarbdialog")
                .setMelding("Melding")
                .setDistribusjonId(1L)
                .build();
        ListenableFuture<SendResult<String, DoknotifikasjonStatus>> send = kvitteringsProducer.send(kvitteringsTopic, melding);
        kvitteringsProducer.flush();
        send.get();

        Awaitility.await().atMost(Duration.of(10, ChronoUnit.SECONDS)).until( () -> {
            String status = null;
            try {
                status = jdbcTemplate.queryForObject("SELECT STATUS FROM EKSTERN_VARSEL_KVITTERING", String.class);

            } catch (EmptyResultDataAccessException e) {
                // ignore
            }
            return "OVERSENDT".equals(status);
        });

    }

//    private void infoOgOVersendtSkalIkkeEndreStatus(String eventId, VarselKvitteringStatus expectedVarselKvitteringStatus) {
//        consumAndAssertStatus(eventId, infoStatus(eventId), expectedVarselKvitteringStatus);
//        consumAndAssertStatus(eventId, oversendtStatus(eventId), expectedVarselKvitteringStatus);
//    }
//
//    private void skalIkkeBehandleMedAnnenBestillingsId(String eventId) {
//        DoknotifikasjonStatus statusMedAnnenBestillerId = okStatus(eventId);
//        statusMedAnnenBestillerId.setBestillerId("annen_bestillerid");
//
//        consumAndAssertStatus(eventId, statusMedAnnenBestillerId, VarselKvitteringStatus.IKKE_SATT);
//    }
//
//
//    private void consumAndAssertStatus(String eventId, DoknotifikasjonStatus message, VarselKvitteringStatus expectedEksternVarselStatus) {
//        String brukernotifikasjonId = BESSKJED_KVOTERINGS_PREFIX + eventId;
//        eksternVarslingKvitteringConsumer.consume(new ConsumerRecord<>("VarselKviteringToppic", 1, 1, brukernotifikasjonId, message));
//
//        assertVarselStatusErSendt(eventId);
//        assertEksternVarselStatus(eventId, expectedEksternVarselStatus);
//    }
//
//    private void assertVarselStatusErSendt(String eventId) {
//        MapSqlParameterSource param = new MapSqlParameterSource()
//                .addValue("eventId", eventId);
//        String status = jdbc.queryForObject("SELECT STATUS from BRUKERNOTIFIKASJON where BRUKERNOTIFIKASJON_ID = :eventId", param, String.class);//TODO fiks denne når vi eksponerer det ut til apiet
//        assertEquals(VarselStatus.SENDT.name(), status);
//    }
//
//    private void assertEksternVarselStatus(String eventId, VarselKvitteringStatus expectedVarselStatus) {
//        MapSqlParameterSource param = new MapSqlParameterSource()
//                .addValue("eventId", eventId);
//        String status = jdbc.queryForObject("SELECT VARSEL_KVITTERING_STATUS from BRUKERNOTIFIKASJON where BRUKERNOTIFIKASJON_ID = :eventId", param, String.class);//TODO fiks denne når vi eksponerer det ut til apiet
//        assertEquals(expectedVarselStatus.name(), status);
//    }
//
//    private DoknotifikasjonStatus status(String eventId, String status) {
//        String bestillingsId = BESSKJED_KVOTERINGS_PREFIX + eventId;
//        return DoknotifikasjonStatus
//                .newBuilder()
//                .setStatus(status)
//                .setBestillingsId(bestillingsId)
//                .setBestillerId("veilarbaktivitet")
//                .setMelding("her er en melding")
//                .setDistribusjonId(1L)
//                .build();
//    }
//
//    private DoknotifikasjonStatus okStatus(String bestillingsId) {
//        return status(bestillingsId, FERDIGSTILT);
//    }
//
//    private DoknotifikasjonStatus feiletStatus(String bestillingsId) {
//        return status(bestillingsId, FEILET);
//    }
//
//    private DoknotifikasjonStatus infoStatus(String bestillingsId) {
//        return status(bestillingsId, INFO);
//    }
//
//    private DoknotifikasjonStatus oversendtStatus(String eventId) {
//        return status(eventId, OVERSENDT);
//    }
//
//    private ConsumerRecord<NokkelInput, OppgaveInput> opprettOppgave(MockBruker mockBruker, AktivitetDTO aktivitetDTO) {
//        brukernotifikasjonAktivitetService.opprettVarselPaaAktivitet(
//                Long.parseLong(aktivitetDTO.getId()),
//                Long.parseLong(aktivitetDTO.getVersjon()),
//                Person.aktorId(mockBruker.getAktorId()),
//                "Testvarsel",
//                VarselType.STILLING_FRA_NAV
//        );
//
//        sendOppgaveCron.sendBrukernotifikasjoner();
//        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();
//
//        assertTrue("Skal ikke produsert done meldinger", kafkaTestService.harKonsumertAlleMeldinger(doneTopic, doneConsumer));
//        final ConsumerRecord<NokkelInput, OppgaveInput> oppgaveRecord = getSingleRecord(oppgaveConsumer, oppgaveTopic, 5000);
//        NokkelInput nokkel = oppgaveRecord.key();
//        OppgaveInput oppgave = oppgaveRecord.value();
//
//        assertEquals(mockBruker.getOppfolgingsperiode().toString(), nokkel.getGrupperingsId());
//        assertEquals(mockBruker.getFnr(), nokkel.getFodselsnummer());
//        assertEquals(basepath + "/aktivitet/vis/" + aktivitetDTO.getId(), oppgave.getLink());
//        return oppgaveRecord;
//    }
}