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
        jdbcTemplate.execute("TRUNCATE TABLE oversikten_utboks")
    }

    @Test
    fun `Gjeldende eskaleringsvarsel som er 14 dager eller eldre skal sendes til oversikten-utboks`() {
        opprettEskaleringsvarselEldreEnn(ZonedDateTime.now().minusDays(14))

        eskaleringsvarselService.sendUtgåtteVarslerTilOversikten()

        val meldingerIUtboks = oversiktenUtboksRepository.hentAlleSomSkalSendes()
        assertThat(meldingerIUtboks).hasSize(1)
        val melding = meldingerIUtboks.first()
        assertThat(melding.fnr.get()).isEqualTo(bruker.fnr)
        assertThat(melding.kategori).isEqualTo(OversiktenMelding.Kategori.UTGATT_VARSEL)
        assertThat(melding.opprettet).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(melding.tidspunktSendt).isNull()
        assertThat(melding.utsendingStatus).isEqualTo(UtsendingStatus.SKAL_SENDES)
        val eskaleringsvarsel = eskaleringsvarselRepository.hentGjeldende(AktorId(bruker.aktorId)).get()
        assertThat(eskaleringsvarsel.sendtTilOversikten).isTrue()
    }

    @Test
    fun `Gjeldende eskaleringsvarsel som er yngre enn 14 dager skal ikke sendes til oversikten-utboks`() {
        opprettEskaleringsvarselEldreEnn(ZonedDateTime.now().minusDays(13))
        eskaleringsvarselService.sendUtgåtteVarslerTilOversikten()
        assertThat(oversiktenUtboksRepository.hentAlleSomSkalSendes()).isEmpty()
    }

    @Test
    fun `ikke-gjeldende eskaleringsvarsel skal ikke sendes til oversikten-utboks`() {
        opprettEskaleringsvarselEldreEnn(ZonedDateTime.now().minusDays(14), false)
        eskaleringsvarselService.sendUtgåtteVarslerTilOversikten()
        assertThat(oversiktenUtboksRepository.hentAlleSomSkalSendes()).isEmpty()
    }

    @Test
    fun `Ikke send eskaleringsvarsel til oversikten hvis den allerede er sendt`() {
        opprettEskaleringsvarselEldreEnn(ZonedDateTime.now().minusDays(20))
        eskaleringsvarselService.sendUtgåtteVarslerTilOversikten()
        val meldingerIUtboks = oversiktenUtboksRepository.hentAlleSomSkalSendes()
        assertThat(meldingerIUtboks).hasSize(1)

        eskaleringsvarselService.sendUtgåtteVarslerTilOversikten()

        assertThat(meldingerIUtboks).hasSize(1)
    }

    @Test
    fun `Melding om stopp skal sendes til oversikten-utboks`() {
        opprettEskaleringsvarselEldreEnn(ZonedDateTime.now().minusDays(20))
        eskaleringsvarselService.sendUtgåtteVarslerTilOversikten()
        val meldingerIUtboks = oversiktenUtboksRepository.hentAlleSomSkalSendes()
        assertThat(meldingerIUtboks).hasSize(1)
        oversiktenService.sendUsendteMeldingerTilOversikten()

        eskaleringsvarselService.stop(StopEskaleringDto(Fnr.of(bruker.fnr), "", false), NavIdent(veileder.navIdent))

        val meldingerIUtboksEtterStopp = oversiktenUtboksRepository.hentAlleSomSkalSendes()
        assertThat(meldingerIUtboksEtterStopp).hasSize(1)
        val melding = meldingerIUtboksEtterStopp.first()
        assertThat(melding.fnr.get()).isEqualTo(bruker.fnr)
        assertThat(melding.kategori).isEqualTo(OversiktenMelding.Kategori.UTGATT_VARSEL)
        assertThat(melding.opprettet).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(melding.tidspunktSendt).isNull()
        assertThat(melding.utsendingStatus).isEqualTo(UtsendingStatus.SKAL_SENDES)
    }

    @Test
    fun `Melding om stopp når oppfølgingsperiode avsluttes skal sendes til oversikten-utboks`() {
        opprettEskaleringsvarselEldreEnn(ZonedDateTime.now().minusDays(20))
        eskaleringsvarselService.sendUtgåtteVarslerTilOversikten()
        val meldingerIUtboks = oversiktenUtboksRepository.hentAlleSomSkalSendes()
        assertThat(meldingerIUtboks).hasSize(1)
        oversiktenService.sendUsendteMeldingerTilOversikten()

        eskaleringsvarselService.stop(bruker.oppfolgingsperiode)

        val meldingerIUtboksEtterStopp = oversiktenUtboksRepository.hentAlleSomSkalSendes()
        assertThat(meldingerIUtboksEtterStopp).hasSize(1)
        val melding = meldingerIUtboksEtterStopp.first()
        assertThat(melding.fnr.get()).isEqualTo(bruker.fnr)
        assertThat(melding.kategori).isEqualTo(OversiktenMelding.Kategori.UTGATT_VARSEL)
        assertThat(melding.opprettet).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(melding.tidspunktSendt).isNull()
        assertThat(melding.utsendingStatus).isEqualTo(UtsendingStatus.SKAL_SENDES)
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