package no.nav.fo.veilarbdialog.eskaleringsvarsel

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.domain.NyMeldingDTO
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StopEskaleringDto
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService
import no.nav.fo.veilarbdialog.oversiktenVaas.OversiktenMelding
import no.nav.fo.veilarbdialog.oversiktenVaas.OversiktenMeldingMedMetadata
import no.nav.fo.veilarbdialog.oversiktenVaas.UtsendingStatus
import no.nav.veilarbaktivitet.veilarbdbutil.VeilarbDialogSqlParameterSource
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class EskaleringsvarselServiceTest: SpringBootTestBase() {

    private val bruker = MockNavService.createHappyBruker()
    private val veileder = MockNavService.createVeileder(bruker)

    @BeforeEach
    fun beforeEach() {
        jdbcTemplate.execute("TRUNCATE TABLE oversikten_melding_med_metadata")
    }

    @Test
    fun `Gjeldende eskaleringsvarsel som er 10 dager eller eldre skal sendes til oversikten`() {
        opprettEskaleringsvarselEldreEnn(ZonedDateTime.now().minusDays(10))

        eskaleringsvarselService.sendUtgåtteVarslerTilOversiktenOutbox()

        val meldinger = hentAlleOversiktenMeldinger()
        assertThat(meldinger).hasSize(1)
        val melding = meldinger.first()
        assertThat(melding.fnr.get()).isEqualTo(bruker.fnr)
        assertThat(melding.kategori).isEqualTo(OversiktenMelding.Kategori.UTGATT_VARSEL)
        assertThat(melding.opprettet).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(melding.utsendingStatus).isEqualTo(UtsendingStatus.SKAL_STARTES)
        val eskaleringsvarsel = eskaleringsvarselRepository.hentGjeldende(AktorId(bruker.aktorId)).get()
        assertThat(eskaleringsvarsel.oversiktenSendingUuid).isEqualTo(melding.meldingKey)
    }

    @Test
    fun `Gjeldende eskaleringsvarsel som er yngre enn 10 dager skal ikke sendes til oversikten`() {
        opprettEskaleringsvarselEldreEnn(ZonedDateTime.now().minusDays(9))
        eskaleringsvarselService.sendUtgåtteVarslerTilOversiktenOutbox()
        assertThat(oversiktenMeldingMedMetadataRepository.hentAlleSomSkalSendes()).isEmpty()
    }

    @Test
    fun `ikke-gjeldende eskaleringsvarsel skal ikke sendes til oversikten`() {
        opprettEskaleringsvarselEldreEnn(ZonedDateTime.now().minusDays(10), false)
        eskaleringsvarselService.sendUtgåtteVarslerTilOversiktenOutbox()
        assertThat(oversiktenMeldingMedMetadataRepository.hentAlleSomSkalSendes()).isEmpty()
    }

    @Test
    fun `Ikke send eskaleringsvarsel til oversikten hvis den allerede er sendt`() {
        opprettEskaleringsvarselEldreEnn(ZonedDateTime.now().minusDays(20))
        eskaleringsvarselService.sendUtgåtteVarslerTilOversiktenOutbox()
        val meldinger = hentAlleOversiktenMeldinger()
        assertThat(meldinger).hasSize(1)

        eskaleringsvarselService.sendUtgåtteVarslerTilOversiktenOutbox()

        assertThat(meldinger).hasSize(1)
    }

    @Test
    fun `Melding om stopp skal lagres i oversiktens utboks`() {
        opprettEskaleringsvarselEldreEnn(ZonedDateTime.now().minusDays(20))
        eskaleringsvarselService.sendUtgåtteVarslerTilOversiktenOutbox()
        val meldingKey = hentAlleOversiktenMeldinger()[0].meldingKey
        oversiktenService.sendUsendteMeldingerTilOversikten()
        assertThat(oversiktenMeldingMedMetadataRepository.hentAlleSomSkalSendes()).hasSize(0)

        eskaleringsvarselService.stop(StopEskaleringDto(Fnr.of(bruker.fnr), "", false), NavIdent(veileder.navIdent))

        val stoppMeldinger = oversiktenMeldingMedMetadataRepository.hent(meldingKey, OversiktenMelding.Operasjon.STOPP)
        assertThat(stoppMeldinger).hasSize(1)
        val melding = stoppMeldinger.first()
        assertThat(melding.fnr.get()).isEqualTo(bruker.fnr)
        assertThat(melding.kategori).isEqualTo(OversiktenMelding.Kategori.UTGATT_VARSEL)
        assertThat(melding.opprettet).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(melding.tidspunktSendt).isNull()
        assertThat(melding.tidspunktStoppet).isNull()
        assertThat(melding.utsendingStatus).isEqualTo(UtsendingStatus.SKAL_STOPPES)
    }


    @Test
    fun `Melding om stopp skal ikke sendes til oversikten dersom startmelding ikke ble sendt`() {
        opprettEskaleringsvarselEldreEnn(ZonedDateTime.now().minusDays(20))
        assertThat(hentAlleOversiktenMeldinger()).hasSize(0)

        eskaleringsvarselService.stop(StopEskaleringDto(Fnr.of(bruker.fnr), "", false), NavIdent(veileder.navIdent))

        val stoppMeldinger = hentAlleOversiktenMeldinger()
        assertThat(stoppMeldinger).hasSize(0)
    }

    @Test
    fun `Melding om stopp når oppfølgingsperiode avsluttes skal sendes til oversikten`() {
        opprettEskaleringsvarselEldreEnn(ZonedDateTime.now().minusDays(20))
        eskaleringsvarselService.sendUtgåtteVarslerTilOversiktenOutbox()
        val meldinger = hentAlleOversiktenMeldinger()
        assertThat(meldinger).hasSize(1)
        val meldingKey = meldinger[0].meldingKey
        oversiktenService.sendUsendteMeldingerTilOversikten()

        eskaleringsvarselService.stoppEskaleringsvarselFordiOppfolgingsperiodenErAvsluttet(bruker.oppfolgingsperiode)

        val stoppMeldinger = oversiktenMeldingMedMetadataRepository.hent(meldingKey, OversiktenMelding.Operasjon.STOPP)
        assertThat(stoppMeldinger).hasSize(1)
        val melding = stoppMeldinger.first()
        assertThat(melding.fnr.get()).isEqualTo(bruker.fnr)
        assertThat(melding.kategori).isEqualTo(OversiktenMelding.Kategori.UTGATT_VARSEL)
        assertThat(melding.opprettet).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(melding.tidspunktSendt).isNull()
        assertThat(melding.tidspunktStoppet).isNull()
        assertThat(melding.utsendingStatus).isEqualTo(UtsendingStatus.SKAL_STOPPES)
    }

    fun hentAlleOversiktenMeldinger(): List<OversiktenMeldingMedMetadata> {
        val sql = """
            SELECT * FROM oversikten_melding_med_metadata
        """.trimIndent()
        return namedParameterJdbcTemplate.query(sql, oversiktenMeldingMedMetadataRepository.rowMapper)
    }

    fun opprettEskaleringsvarselEldreEnn(tidspunkt: ZonedDateTime, erGjeldende : Boolean = true) {
        dialogTestService.opprettDialogSomVeileder(veileder, bruker, NyMeldingDTO().setTekst("").setOverskrift(""))
        dialogTestService.startEskalering(veileder, StartEskaleringDto(Fnr.of(bruker.fnr),"", "", "", "" ))

        val sqlGjeldende = """
            UPDATE eskaleringsvarsel
            SET opprettet_dato = :dato
            WHERE gjeldende = :aktorId
        """.trimIndent()

        val sqlIkkeGjeldende = """
            UPDATE eskaleringsvarsel
            SET opprettet_dato = :dato,
            gjeldende = null,  
            avsluttet_dato = NOW()
            WHERE gjeldende = :aktorId
        """.trimIndent()
        val sql = if(erGjeldende) sqlGjeldende else sqlIkkeGjeldende
        val params = VeilarbDialogSqlParameterSource()
            .addValue("dato", tidspunkt)
            .addValue("aktorId", bruker.aktorId)
        namedParameterJdbcTemplate.update(sql, params)
    }
}