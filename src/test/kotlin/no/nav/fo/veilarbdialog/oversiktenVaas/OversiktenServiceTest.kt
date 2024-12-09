package no.nav.fo.veilarbdialog.oversiktenVaas

import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.*

open class OversiktenServiceTest: SpringBootTestBase() {

    @MockBean
    private lateinit var oversiktenProducer: OversiktenProducer

    private val bruker = MockNavService.createHappyBruker()

    @BeforeEach
    fun beforeEach() {
        jdbcTemplate.execute("TRUNCATE TABLE oversikten_melding_med_metadata")
    }

    @Test
    fun `Skal sende usendte meldinger`() {
        val melding = melding(bruker, utsendingStatus = UtsendingStatus.SKAL_SENDES)

        val sendtMelding = melding(bruker, utsendingStatus = UtsendingStatus.SENDT)

        oversiktenMeldingMedMetadataRepository.lagre(melding)
        oversiktenMeldingMedMetadataRepository.lagre(sendtMelding)

        oversiktenService.sendUsendteMeldingerTilOversikten()

        verify(oversiktenProducer, Mockito.times(1))
            .sendMelding(melding.meldingKey.toString(), melding.meldingSomJson)
        verify(oversiktenProducer, Mockito.times(1))
            .sendMelding(sendtMelding.meldingKey.toString(), sendtMelding.meldingSomJson)
        verifyNoMoreInteractions(oversiktenProducer)
    }

    @Test
    fun `Skal ikke sende melding som er markert som SENDT`() {
        val melding = melding(bruker, utsendingStatus = UtsendingStatus.SENDT)
        oversiktenMeldingMedMetadataRepository.lagre(melding)

        oversiktenService.sendUsendteMeldingerTilOversikten()

        Mockito.verifyNoInteractions(oversiktenProducer)
    }

    @Test
    fun `Skal ikke sende melding som er markert som SKAL_IKKE_SENDES`() {
        val melding = melding(bruker, utsendingStatus = UtsendingStatus.SKAL_IKKE_SENDES)
        oversiktenMeldingMedMetadataRepository.lagre(melding)

        oversiktenService.sendUsendteMeldingerTilOversikten()

        Mockito.verifyNoInteractions(oversiktenProducer)
    }


    @Test
    fun `Melding om stopp skal ikke påvirke start-melding`() {
        val melding = melding(bruker, utsendingStatus = UtsendingStatus.SKAL_SENDES)
        oversiktenMeldingMedMetadataRepository.lagre(melding)

        oversiktenService.sendUsendteMeldingerTilOversikten()

        Mockito.verifyNoInteractions(oversiktenProducer)
    }

    private fun melding(bruker: MockBruker, meldingKey: UUID = UUID.randomUUID(), utsendingStatus: UtsendingStatus = UtsendingStatus.SKAL_SENDES) =
        OversiktenMeldingMedMetadata(
            fnr = Fnr.of(bruker.fnr),
            meldingSomJson = "{}",
            kategori = OversiktenMelding.Kategori.UTGATT_VARSEL,
            meldingKey = meldingKey,
            utsendingStatus = utsendingStatus
        )
}