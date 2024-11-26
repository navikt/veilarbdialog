package no.nav.fo.veilarbdialog.oversiktenVaas

import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.*

open class OversiktenServiceTest: SpringBootTestBase() {

    @Autowired
    private lateinit var oversiktenService: OversiktenService

    @MockBean
    private lateinit var oversiktenProducer: OversiktenProducer

    private val bruker = MockNavService.createHappyBruker()

    @BeforeEach
    fun beforeEach() {
        jdbcTemplate.execute("TRUNCATE TABLE oversikten_vaas_utboks")
    }

    @Test
    fun `Skal sende usendte meldinger`() {
        val melding = melding(bruker)
        oversiktenUtboksRepository.lagreSending(melding)

        oversiktenService.sendUsendteMeldingerTilOversikten()

        Mockito.verify(oversiktenProducer, Mockito.times(1))
            .sendMelding(melding.meldingKey.toString(), melding.meldingSomJson)
    }

    @Test
    fun `Skal ikke sende melding som er markert som SENDT`() {
        val melding = melding(bruker, utsendingStatus = UtsendingStatus.SENDT)
        oversiktenUtboksRepository.lagreSending(melding)

        oversiktenService.sendUsendteMeldingerTilOversikten()

        Mockito.verifyNoInteractions(oversiktenProducer)
    }

    @Test
    fun `Skal ikke sende melding som er markert som SKAL_IKKE_SENDES`() {
        val melding = melding(bruker, utsendingStatus = UtsendingStatus.SKAL_IKKE_SENDES)
        oversiktenUtboksRepository.lagreSending(melding)

        oversiktenService.sendUsendteMeldingerTilOversikten()

        Mockito.verifyNoInteractions(oversiktenProducer)
    }

    private fun melding(bruker: MockBruker, utsendingStatus: UtsendingStatus = UtsendingStatus.SKAL_SENDES) =
        SendingEntity(
            fnr = Fnr.of(bruker.fnr),
            meldingSomJson = "{}",
            kategori = OversiktenMelding.Kategori.UTGATT_VARSEL,
            meldingKey = UUID.randomUUID(),
            utsendingStatus = utsendingStatus
        )
}