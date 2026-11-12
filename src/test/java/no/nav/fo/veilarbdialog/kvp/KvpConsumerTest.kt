package no.nav.fo.veilarbdialog.kvp

import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.domain.NyMeldingDTO
import no.nav.fo.veilarbdialog.domain.kafka.KvpAvsluttet
import no.nav.fo.veilarbdialog.domain.kafka.KvpPeriode
import no.nav.fo.veilarbdialog.domain.kafka.KvpPeriodeEventType
import no.nav.fo.veilarbdialog.domain.kafka.KvpStartet
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService
import no.nav.fo.veilarbdialog.util.DialogTestService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class KvpConsumerTest(
    @Autowired
    val dialogTestService: DialogTestService
) : SpringBootTestBase() {
    companion object {
        private const val SAKSBEHANDLER = "Z99999"
        private const val BEGRUNNELSE = "Derfor"
    }

    @Autowired
    lateinit var producer: KafkaTemplate<String, String>

    @Value("\${application.topic.inn.kvpperioder}")
    lateinit var kvpPerioderTopic: String

    @Test
    fun `avslutning av KVP perioder skal sette dialoger til historisk`() {
        val mockBruker = MockNavService.createHappyBruker()
        startKvpPåBruker(mockBruker)
        val dialog = dialogTestService.opprettDialogSomBruker(
            mockBruker, NyMeldingDTO().setTekst("henvendelse kvp").setOverskrift("Overskrift")
        )
        avsluttKvpPåBruker(mockBruker)
        val dialogEtterAvslutning = dialogTestService.hentDialog(mockBruker, dialog.id.toLong())
        assertThat(dialogEtterAvslutning.isHistorisk).isTrue()
    }

    private fun startKvpPåBruker(mockBruker: MockBruker) {
        `when`(this.kvpService.kontorsperreEnhetId(Fnr.of(mockBruker.fnr))).thenReturn("2121")
        val startet = KvpPeriode(
            event = KvpPeriodeEventType.STARTET,
            enhetId = "2121",
            avsluttet = null,
            aktorId = mockBruker.aktorId,
            startet = KvpStartet (
                    SAKSBEHANDLER,
            ZonedDateTime.now(),
            BEGRUNNELSE
            )
        )
        val sendResult = producer.send(kvpPerioderTopic, mockBruker.aktorId, JsonUtils.toJson(startet))
            .get(1, TimeUnit.SECONDS)
        waitUntilConsumed(sendResult)
    }

    private fun avsluttKvpPåBruker(mockBruker: MockBruker) {
        `when`(this.kvpService.kontorsperreEnhetId(Fnr.of(mockBruker.fnr))).thenReturn(null)
        val avsluttet = KvpPeriode(
            event = KvpPeriodeEventType.AVSLUTTET,
            enhetId = "2121",
            aktorId = mockBruker.aktorId,
            startet = KvpStartet (
                SAKSBEHANDLER,
                ZonedDateTime.now().minusDays(1),
                BEGRUNNELSE
            ),
            avsluttet = KvpAvsluttet(
            SAKSBEHANDLER,
            ZonedDateTime.now(),
            BEGRUNNELSE
            )
        )
        val sendResult = producer.send(kvpPerioderTopic, mockBruker.aktorId, JsonUtils.toJson(avsluttet))
            .get(1, TimeUnit.SECONDS)
        waitUntilConsumed(sendResult)
    }

    private fun waitUntilConsumed(sendResult: SendResult<String, String>) {
        kafkaTestService.assertErKonsumertAiven(
            kvpPerioderTopic,
            sendResult.recordMetadata.offset(),
            sendResult.recordMetadata.partition(),
            5
        )
    }

}
