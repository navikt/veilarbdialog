package no.nav.fo.veilarbdialog.oppfolging.siste_periode;

import lombok.SneakyThrows;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.SpringBootTestBase;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonBehandlingStatus;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonRepository;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType;
import no.nav.fo.veilarbdialog.brukernotifikasjon.entity.BrukernotifikasjonEntity;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.EskaleringsvarselService;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.EskaleringsvarselDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import org.apache.kafka.clients.consumer.Consumer;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class OppfolgingsperiodeConsumerTest extends SpringBootTestBase {

    @Autowired
    KafkaTemplate<String, String> producer;

    Consumer<String, String> endringPaaDialogConsumer;

    @Value("${application.kafka.endringPaaDialogTopic}")
    String endringPaaDialogTopic;

    @Value("${application.topic.inn.oppfolgingsperiode}")
    String oppfolgingsperiodeTopic;

    @LocalServerPort
    private int port;

    @Autowired
    private SistePeriodeDAO sistePeriodeDAO;

    @Autowired
    BrukernotifikasjonService brukernotifikasjonService;

    @Autowired
    BrukernotifikasjonRepository brukernotifikasjonRepository;

    @Autowired
    EskaleringsvarselService eskaleringsvarselService;


    @Before
    public void setup() {
        this.endringPaaDialogConsumer = kafkaTestService.createStringStringConsumer(endringPaaDialogTopic);
    }

    @Test
    public void skal_opprette_siste_oppfolgingsperiode() throws InterruptedException, ExecutionException, TimeoutException {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        String aktorId = mockBruker.getAktorId();
        UUID oppfolgingsId = mockBruker.getOppfolgingsperiode();


        OppfolgingsperiodeV1 startOppfolgiong = OppfolgingsperiodeV1.builder()
                .uuid(oppfolgingsId)
                .aktorId(aktorId)
                .startDato(ZonedDateTime.now().minusHours(1).truncatedTo(MILLIS))
                .build();
        SendResult<String, String> sendResult = producer.send(oppfolgingsperiodeTopic, aktorId, JsonUtils.toJson(startOppfolgiong)).get(1, SECONDS);
        kafkaTestService.assertErKonsumertAiven(oppfolgingsperiodeTopic, sendResult.getRecordMetadata().offset(), sendResult.getRecordMetadata().partition(), 10);


        Oppfolgingsperiode oppfolgingsperiode = sistePeriodeDAO.hentSisteOppfolgingsPeriode(aktorId).orElseThrow();
        Assertions.assertThat(oppfolgingsperiode.oppfolgingsperiode()).isEqualTo(oppfolgingsId);
        Assertions.assertThat(oppfolgingsperiode.aktorid()).isEqualTo(aktorId);
        Assertions.assertThat(oppfolgingsperiode.startTid()).isEqualTo(startOppfolgiong.getStartDato());
        Assertions.assertThat(oppfolgingsperiode.sluttTid()).isNull();


        OppfolgingsperiodeV1 avsluttetOppfolgingsperide = startOppfolgiong.withSluttDato(ZonedDateTime.now().truncatedTo(MILLIS));
        SendResult<String, String> avsluttetSendResult = producer.send(oppfolgingsperiodeTopic, aktorId, JsonUtils.toJson(avsluttetOppfolgingsperide)).get(1, SECONDS);

        kafkaTestService.assertErKonsumertAiven(oppfolgingsperiodeTopic, avsluttetSendResult.getRecordMetadata().offset(), sendResult.getRecordMetadata().partition(), 10);

        Oppfolgingsperiode oppfolgingsperiodeAvsluttet = sistePeriodeDAO.hentSisteOppfolgingsPeriode(aktorId).orElseThrow();
        Assertions.assertThat(oppfolgingsperiodeAvsluttet.oppfolgingsperiode()).isEqualTo(oppfolgingsId);
        Assertions.assertThat(oppfolgingsperiodeAvsluttet.aktorid()).isEqualTo(aktorId);
        Assertions.assertThat(oppfolgingsperiodeAvsluttet.startTid()).isEqualTo(avsluttetOppfolgingsperide.getStartDato());
        Assertions.assertThat(oppfolgingsperiodeAvsluttet.sluttTid()).isEqualTo(avsluttetOppfolgingsperide.getSluttDato());
    }

    @Test
    @SneakyThrows
    public void skal_avslutte_gjeldende_varsler_og_notifikasjoner() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        String aktorId = mockBruker.getAktorId();
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);

        OppfolgingsperiodeV1 startOppfolging = OppfolgingsperiodeV1.builder()
                .uuid(mockBruker.getOppfolgingsperiode())
                .aktorId(aktorId)
                .startDato(ZonedDateTime.now().minusDays(5).truncatedTo(MILLIS))
                .build();

        opprettEllerEndreOppfolgingsperiodeForBruker(startOppfolging);

        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(mockBruker.getFnr()), "begrunnelse", "overskrift", "henvendelseTekst");
        EskaleringsvarselDto startEskalering = dialogTestService.startEskalering(mockVeileder, startEskaleringDto);

        OppfolgingsperiodeV1 stopOppfolging = OppfolgingsperiodeV1.builder()
                .uuid(mockBruker.getOppfolgingsperiode())
                .aktorId(aktorId)
                .startDato(startOppfolging.getStartDato())
                .sluttDato(ZonedDateTime.now().truncatedTo(MILLIS))
                .build();

        opprettEllerEndreOppfolgingsperiodeForBruker(stopOppfolging);

        BrukernotifikasjonEntity brukernotifikasjon = brukernotifikasjonRepository.hentBrukernotifikasjonForDialogId(startEskalering.tilhorendeDialogId(), BrukernotifikasjonsType.OPPGAVE).get(0);

        Assertions.assertThat(brukernotifikasjon.status()).isEqualTo(BrukernotifikasjonBehandlingStatus.SKAL_AVSLUTTES);

        List<EskaleringsvarselEntity> historikk = eskaleringsvarselService.historikk(Fnr.of(mockBruker.getFnr()));
        Assertions.assertThat(historikk).hasSize(1);
        Assertions.assertThat(historikk.get(0).avsluttetDato()).isNotNull();
        Assertions.assertThat(historikk.get(0).avsluttetAv()).isEqualTo("SYSTEM");
        Assertions.assertThat(historikk.get(0).avsluttetBegrunnelse()).isEqualToIgnoringCase("OPPFOLGING AVSLUTTET");

    }

    @Test
    public void skal_sette_dialoger_til_historisk() throws ExecutionException, InterruptedException, TimeoutException {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        String aktorId = mockBruker.getAktorId();

        OppfolgingsperiodeV1 startOppfolging = OppfolgingsperiodeV1.builder()
                .uuid(mockBruker.getOppfolgingsperiode())
                .aktorId(aktorId)
                .startDato(ZonedDateTime.now().minusDays(5).truncatedTo(MILLIS))
                .build();

        opprettEllerEndreOppfolgingsperiodeForBruker(startOppfolging);

        DialogDTO dialogDTO = dialogTestService.opprettDialogSomBruker(port, mockBruker, new NyHenvendelseDTO().setOverskrift("The Three Trials").setTekst("Defeat the Sword Master of Melee Island"));

        OppfolgingsperiodeV1 stopOppfolging = OppfolgingsperiodeV1.builder()
                .uuid(mockBruker.getOppfolgingsperiode())
                .aktorId(aktorId)
                .startDato(startOppfolging.getStartDato())
                .sluttDato(ZonedDateTime.now().truncatedTo(MILLIS))
                .build();

        opprettEllerEndreOppfolgingsperiodeForBruker(stopOppfolging);

        KafkaTestUtils.getSingleRecord(endringPaaDialogConsumer, endringPaaDialogTopic, 10000);


        DialogDTO hentDialog = dialogTestService.hentDialog(port, mockBruker, Long.parseLong(dialogDTO.getId()));

        Assertions.assertThat(hentDialog.isHistorisk()).isTrue();
    }

    private void opprettEllerEndreOppfolgingsperiodeForBruker(OppfolgingsperiodeV1 oppfolgingsperiode) throws ExecutionException, InterruptedException, TimeoutException {
        SendResult<String, String> sendResult = producer.send(oppfolgingsperiodeTopic, oppfolgingsperiode.getAktorId(), JsonUtils.toJson(oppfolgingsperiode)).get(1, SECONDS);
        kafkaTestService.assertErKonsumertAiven(oppfolgingsperiodeTopic, sendResult.getRecordMetadata().offset(), sendResult.getRecordMetadata().partition(), 10);
    }

}