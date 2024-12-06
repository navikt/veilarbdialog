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
        val startMelding = melding(bruker, utsendingStatus = UtsendingStatus.SKAL_STARTES)
        val stoppMelding = melding(bruker, utsendingStatus = UtsendingStatus.SKAL_STOPPES)

        val startetMelding = melding(bruker, utsendingStatus = UtsendingStatus.STARTET)
        val stoppetMelding = melding(bruker, utsendingStatus = UtsendingStatus.STOPPET)

        oversiktenMeldingMedMetadataRepository.lagre(startMelding)
        oversiktenMeldingMedMetadataRepository.lagre(stoppMelding)
        oversiktenMeldingMedMetadataRepository.lagre(startetMelding)
        oversiktenMeldingMedMetadataRepository.lagre(stoppetMelding)

        oversiktenService.sendUsendteMeldingerTilOversikten()

        verify(oversiktenProducer, Mockito.times(1))
            .sendMelding(startMelding.meldingKey.toString(), startMelding.meldingSomJson)
        verify(oversiktenProducer, Mockito.times(1))
            .sendMelding(stoppMelding.meldingKey.toString(), stoppMelding.meldingSomJson)
        verifyNoMoreInteractions(oversiktenProducer)
    }

    @Test
    fun `Skal ikke sende melding som er markert som STARTET`() {
        val melding = melding(bruker, utsendingStatus = UtsendingStatus.STARTET)
        oversiktenMeldingMedMetadataRepository.lagre(melding)

        oversiktenService.sendUsendteMeldingerTilOversikten()

        Mockito.verifyNoInteractions(oversiktenProducer)
    }

    @Test
    fun `Skal ikke sende melding som er markert som STOPPET`() {
        val melding = melding(bruker, utsendingStatus = UtsendingStatus.STOPPET)
        oversiktenMeldingMedMetadataRepository.lagre(melding)

        oversiktenService.sendUsendteMeldingerTilOversikten()

        Mockito.verifyNoInteractions(oversiktenProducer)
    }

    @Test
    fun `Skal ikke sende melding som er markert som er ABORTERT`() {
        val melding = melding(bruker, utsendingStatus = UtsendingStatus.ABORTERT)
        oversiktenMeldingMedMetadataRepository.lagre(melding)

        oversiktenService.sendUsendteMeldingerTilOversikten()

        Mockito.verifyNoInteractions(oversiktenProducer)
    }

    private fun melding(bruker: MockBruker, meldingKey: UUID = UUID.randomUUID(), utsendingStatus: UtsendingStatus = UtsendingStatus.SKAL_STARTES) =
        OversiktenMeldingMedMetadata(
            fnr = Fnr.of(bruker.fnr),
            meldingSomJson = "{}",
            kategori = OversiktenMelding.Kategori.UTGATT_VARSEL,
            meldingKey = meldingKey,
            utsendingStatus = utsendingStatus
        )
}