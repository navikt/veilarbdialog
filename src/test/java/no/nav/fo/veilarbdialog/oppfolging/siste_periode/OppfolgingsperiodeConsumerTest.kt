package no.nav.fo.veilarbdialog.oppfolging.siste_periode

import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.domain.NyMeldingDTO
import no.nav.fo.veilarbdialog.eskaleringsvarsel.EskaleringsvarselService
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto
import no.nav.fo.veilarbdialog.minsidevarsel.MinSideVarselBehandlingStatus
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinsideVarselDao
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService
import no.nav.fo.veilarbdialog.util.KafkaTestService
import org.apache.kafka.clients.consumer.Consumer
import org.assertj.core.api.Assertions
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

internal class OppfolgingsperiodeConsumerTest(
    @Autowired
    val producer: KafkaTemplate<String, String>,
    @Value("\${application.topic.ut.endringPaaDialog}")
    val endringPaaDialogTopic: String,
    @Value("\${application.topic.inn.oppfolgingsperiode}")
    val oppfolgingsperiodeTopic: String,
    @Autowired
    private val sistePeriodeDAO: SistePeriodeDAO,
    @Autowired
    val minsideVarselDao: MinsideVarselDao,
    @Autowired
    val eskaleringsvarselService: EskaleringsvarselService
) : SpringBootTestBase() {

    lateinit var endringPaaDialogConsumer: Consumer<String, String>

    @BeforeEach
    fun setupConsumer() {
        this.endringPaaDialogConsumer = kafkaTestService.createStringStringConsumer(endringPaaDialogTopic)
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun `skal lagre siste oppfolgingsperiode selv om den er avsluttet`() {
        val mockBruker = MockNavService.createHappyBruker()
        val aktorId = mockBruker.aktorId
        val oppfolgingsId = mockBruker.oppfolgingsperiode

        val førstePeriode = startOppfolgingsperiodeForBruker(mockBruker, ZonedDateTime.now().minusHours(1).truncatedTo(ChronoUnit.MILLIS))

        val oppfolgingsperiode = sistePeriodeDAO.hentSisteOppfolgingsPeriode(aktorId).orElseThrow()
        Assertions.assertThat(oppfolgingsperiode.oppfolgingsperiode).isEqualTo(oppfolgingsId)
        Assertions.assertThat(oppfolgingsperiode.aktorid).isEqualTo(aktorId)
        Assertions.assertThat(oppfolgingsperiode.startTid).isEqualTo(førstePeriode.startDato)
        Assertions.assertThat(oppfolgingsperiode.sluttTid).isNull()

        val avsluttPeriodeMelding = avsluttOppfolging(mockBruker, førstePeriode.startDato)

        val oppfolgingsperiodeAvsluttet = sistePeriodeDAO.hentSisteOppfolgingsPeriode(aktorId).orElseThrow()
        Assertions.assertThat(mockBruker.oppfolgingsperiode).isEqualTo(oppfolgingsId)
        Assertions.assertThat(mockBruker.aktorId).isEqualTo(aktorId)
        Assertions.assertThat(oppfolgingsperiodeAvsluttet.startTid).isEqualTo(førstePeriode.startDato)
        Assertions.assertThat(oppfolgingsperiodeAvsluttet.sluttTid).isEqualTo(avsluttPeriodeMelding.sluttDato)
    }

    @Test
    fun `skal avslutte gjeldende varsler og notifikasjoner`() {
        val mockBruker = MockNavService.createHappyBruker()
        val mockVeileder = MockNavService.createVeileder(mockBruker)
        val periode = startOppfolgingsperiodeForBruker(mockBruker)

        val startEskaleringDto =
            StartEskaleringDto(Fnr.of(mockBruker.fnr), "begrunnelse", "overskrift", "henvendelseTekst", null)
        val startEskalering = dialogTestService.startEskalering(mockVeileder, startEskaleringDto)

        avsluttOppfolging(mockBruker, periode.startDato)

        val varselStatus = minsideVarselDao.getMinsideVarselForForhåndsvarsel(startEskalering.id)

        AssertionsForClassTypes.assertThat(varselStatus.status)
            .isEqualTo(MinSideVarselBehandlingStatus.SKAL_AVSLUTTES)
        val historikk = eskaleringsvarselService.historikk(Fnr.of(mockBruker.fnr))
        Assertions.assertThat(historikk).hasSize(1)
        AssertionsForClassTypes.assertThat(historikk.first().avsluttetDato).isNotNull()
        AssertionsForClassTypes.assertThat(historikk.first().avsluttetAv).isEqualTo("SYSTEM")
        AssertionsForClassTypes.assertThat(historikk.first().avsluttetBegrunnelse)
            .isEqualToIgnoringCase("OPPFOLGING AVSLUTTET")
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun `skal sette dialoger til historisk`() {
        val mockBruker = MockNavService.createHappyBruker()
        val periode = startOppfolgingsperiodeForBruker(mockBruker)

        val dialogDTO = dialogTestService.opprettDialogSomBruker(
            mockBruker,
            NyMeldingDTO().setOverskrift("The Three Trials").setTekst("Defeat the Sword Master of Melee Island")
        )

        avsluttOppfolging(mockBruker, periode.startDato)

        outboxService.sendUsendteMeldinger()
        KafkaTestUtils.getSingleRecord(
            endringPaaDialogConsumer,
            endringPaaDialogTopic,
            KafkaTestService.DEFAULT_WAIT_TIMEOUT
        )

        val hentDialog = dialogTestService.hentDialog(mockBruker, dialogDTO.id.toLong())
        Assertions.assertThat(hentDialog.isHistorisk).isTrue()
    }

    @Test
    fun `skal publisere tombstone på siste-hendelse topic når oppfølging avsluttes`() {
        val mockBruker = MockNavService.createHappyBruker()
        val sisteHendelseConsumer = kafkaTestService.createStringStringConsumer(sisteHendelseTopic)
        val periode = startOppfolgingsperiodeForBruker(mockBruker)

        avsluttOppfolging(mockBruker, periode.startDato)

        val records = kafkaTestService.consumeNewRecords(sisteHendelseTopic, sisteHendelseConsumer, 1)
        Assertions.assertThat(records.last().key()).isEqualTo(mockBruker.aktorId)
        Assertions.assertThat(records.last().value()).isNull()
    }

    private fun avsluttOppfolging(mockBruker: MockBruker, startDato: ZonedDateTime): OppfolgingsperiodeV1 {
        val stopOppfolging = OppfolgingsperiodeV1.builder()
            .uuid(mockBruker.oppfolgingsperiode)
            .aktorId(mockBruker.aktorId)
            .startDato(startDato)
            .sluttDato(ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS))
            .build()
        opprettEllerEndreOppfolgingsperiodeForBruker(stopOppfolging)
        return stopOppfolging
    }

    private fun startOppfolgingsperiodeForBruker(mockBruker: MockBruker, startDato: ZonedDateTime? = null): OppfolgingsperiodeV1 {
        val startOppfolging = OppfolgingsperiodeV1.builder()
            .uuid(mockBruker.oppfolgingsperiode)
            .aktorId(mockBruker.aktorId)
            .startDato(startDato ?: ZonedDateTime.now().minusDays(5).truncatedTo(ChronoUnit.MILLIS))
            .build()
        opprettEllerEndreOppfolgingsperiodeForBruker(startOppfolging)
        return startOppfolging
    }

    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    private fun opprettEllerEndreOppfolgingsperiodeForBruker(oppfolgingsperiode: OppfolgingsperiodeV1) {
        val sendResult = producer.send(
            oppfolgingsperiodeTopic,
            oppfolgingsperiode.getAktorId(),
            JsonUtils.toJson(oppfolgingsperiode)
        ).get(1, TimeUnit.SECONDS)
        kafkaTestService.assertErKonsumertAiven(
            oppfolgingsperiodeTopic,
            sendResult.recordMetadata.offset(),
            sendResult.recordMetadata.partition(),
            10
        )
    }
}