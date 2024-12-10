package no.nav.fo.veilarbdialog.oversiktenVaas

import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.*
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
        verifyNoMoreInteractions(oversiktenProducer)
    }

    @Test
    fun `Skal ikke sende melding som er markert som SENDT`() {
        val melding = melding(bruker, utsendingStatus = UtsendingStatus.SENDT)
        oversiktenMeldingMedMetadataRepository.lagre(melding)

        oversiktenService.sendUsendteMeldingerTilOversikten()

        verifyNoInteractions(oversiktenProducer)
    }

    @Test
    fun `Skal ikke sende melding som er markert som SKAL_IKKE_SENDES`() {
        val melding = melding(bruker, utsendingStatus = UtsendingStatus.SKAL_IKKE_SENDES)
        oversiktenMeldingMedMetadataRepository.lagre(melding)

        oversiktenService.sendUsendteMeldingerTilOversikten()

        verifyNoInteractions(oversiktenProducer)
    }


    @Test
    fun `Nye meldinger skal ikke påvirke andre meldinger`() {
        val førsteMelding = melding(bruker, utsendingStatus = UtsendingStatus.SENDT)
        val førsteMeldingID = oversiktenMeldingMedMetadataRepository.lagre(førsteMelding)
        val andreMelding = melding(meldingKey = førsteMelding.meldingKey, bruker = bruker, utsendingStatus = UtsendingStatus.SKAL_SENDES)
        oversiktenMeldingMedMetadataRepository.lagre(andreMelding)

        oversiktenService.sendUsendteMeldingerTilOversikten()

        val førsteMeldingEtterAndreMeldingErSendt = oversiktenMeldingMedMetadataRepository.hent(førsteMeldingID)
        assertThat(førsteMelding.tidspunktSendt).isEqualTo(førsteMeldingEtterAndreMeldingErSendt.tidspunktSendt)
        assertThat(førsteMelding.fnr).isEqualTo(førsteMeldingEtterAndreMeldingErSendt.fnr)
        assertThat(førsteMelding.meldingSomJson).isEqualTo(førsteMeldingEtterAndreMeldingErSendt.meldingSomJson)
        assertThat(førsteMelding.kategori).isEqualTo(førsteMeldingEtterAndreMeldingErSendt.kategori)
        assertThat(førsteMelding.operasjon).isEqualTo(førsteMeldingEtterAndreMeldingErSendt.operasjon)
        assertThat(førsteMelding.opprettet).isEqualTo(førsteMeldingEtterAndreMeldingErSendt.opprettet)
        assertThat(førsteMelding.utsendingStatus).isEqualTo(førsteMeldingEtterAndreMeldingErSendt.utsendingStatus)
        assertThat(førsteMelding.meldingKey).isEqualTo(førsteMeldingEtterAndreMeldingErSendt.meldingKey)
    }

    private fun melding(bruker: MockBruker, meldingKey: UUID = UUID.randomUUID(), utsendingStatus: UtsendingStatus = UtsendingStatus.SKAL_SENDES) =
        OversiktenMeldingMedMetadata(
            fnr = Fnr.of(bruker.fnr),
            meldingSomJson = "{}",
            kategori = OversiktenMelding.Kategori.UTGATT_VARSEL,
            meldingKey = meldingKey,
            utsendingStatus = utsendingStatus,
            operasjon = OversiktenMelding.Operasjon.START
        )
}