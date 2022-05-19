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
import no.nav.fo.veilarbdialog.eskaleringsvarsel.EskaleringsvarselService;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.EskaleringsvarselDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class SisteOppfolgingsperiodeConsumerTest extends SpringBootTestBase {

    @Autowired
    KafkaTemplate<String, String> producer;

    @Value("${application.topic.inn.sisteOppfolgingsperiode}")
    String oppfolgingSistePeriodeTopic;

    @Autowired
    private SistePeriodeDAO sistePeriodeDAO;

    @Autowired
    BrukernotifikasjonService brukernotifikasjonService;

    @Autowired
    BrukernotifikasjonRepository brukernotifikasjonRepository;

    @Autowired
    EskaleringsvarselService eskaleringsvarselService;

    @Test
    public void skal_opprette_siste_oppfolgingsperiode() throws InterruptedException, ExecutionException, TimeoutException {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        UUID oppfolgingsId = mockBruker.getOppfolgingsperiode();
        String aktorId = mockBruker.getAktorId();


        SisteOppfolgingsperiodeV1 startOppfolgiong = SisteOppfolgingsperiodeV1.builder()
                .uuid(oppfolgingsId)
                .aktorId(aktorId)
                .startDato(ZonedDateTime.now().minusHours(1).truncatedTo(MILLIS))
                .build();
        SendResult<String, String> sendResult = producer.send(oppfolgingSistePeriodeTopic, aktorId, JsonUtils.toJson(startOppfolgiong)).get(1, SECONDS);
        kafkaTestService.assertErKonsumertAiven(oppfolgingSistePeriodeTopic, sendResult.getRecordMetadata().offset(),  sendResult.getRecordMetadata().partition(),10);


        Oppfolgingsperiode oppfolgingsperiode = sistePeriodeDAO.hentSisteOppfolgingsPeriode(aktorId).orElseThrow();
        Assertions.assertThat(oppfolgingsperiode.oppfolgingsperiode()).isEqualTo(oppfolgingsId);
        Assertions.assertThat(oppfolgingsperiode.aktorid()).isEqualTo(aktorId);
        Assertions.assertThat(oppfolgingsperiode.startTid()).isEqualTo(startOppfolgiong.getStartDato());
        Assertions.assertThat(oppfolgingsperiode.sluttTid()).isNull();


        SisteOppfolgingsperiodeV1 avsluttetOppfolgingsperide = startOppfolgiong.withSluttDato(ZonedDateTime.now().truncatedTo(MILLIS));
        SendResult<String, String> avsluttetSendResult = producer.send(oppfolgingSistePeriodeTopic, aktorId, JsonUtils.toJson(avsluttetOppfolgingsperide)).get(1, SECONDS);

        kafkaTestService.assertErKonsumertAiven(oppfolgingSistePeriodeTopic, avsluttetSendResult.getRecordMetadata().offset(), sendResult.getRecordMetadata().partition(),10);

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
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);
        String aktorId = mockBruker.getAktorId();

        SisteOppfolgingsperiodeV1 startOppfolging = SisteOppfolgingsperiodeV1.builder()
                .uuid(mockBruker.getOppfolgingsperiode())
                .aktorId(aktorId)
                .startDato(ZonedDateTime.now().minusDays(5).truncatedTo(MILLIS))
                .build();

        opprettEllerEndreOppfolgingsperiodeForBruker(startOppfolging);

        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(mockBruker.getFnr()), "begrunnelse", "overskrift", "henvendelseTekst");
        EskaleringsvarselDto startEskalering = dialogTestService.startEskalering(mockVeileder, startEskaleringDto);

        SisteOppfolgingsperiodeV1 stopOppfolging = SisteOppfolgingsperiodeV1.builder()
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

    private void opprettEllerEndreOppfolgingsperiodeForBruker(SisteOppfolgingsperiodeV1 oppfolgingsperiode) throws ExecutionException, InterruptedException, TimeoutException {
        SendResult<String, String> sendResult = producer.send(oppfolgingSistePeriodeTopic, oppfolgingsperiode.getAktorId(), JsonUtils.toJson(oppfolgingsperiode)).get(1, SECONDS);
        kafkaTestService.assertErKonsumertAiven(oppfolgingSistePeriodeTopic, sendResult.getRecordMetadata().offset(),  sendResult.getRecordMetadata().partition(),10);
    }

}