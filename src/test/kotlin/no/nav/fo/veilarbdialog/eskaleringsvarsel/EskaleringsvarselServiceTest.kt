package no.nav.fo.veilarbdialog.eskaleringsvarsel

import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.domain.NyMeldingDTO
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService
import no.nav.veilarbaktivitet.veilarbdbutil.VeilarbDialogSqlParameterSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class EskaleringsvarselServiceTest: SpringBootTestBase() {

    private val bruker = MockNavService.createHappyBruker()
    private val veileder = MockNavService.createVeileder(bruker)

    @BeforeEach
    fun beforeEach() {
        jdbcTemplate.execute("TRUNCATE TABLE oversikten_vaas_utboks")
    }

    @Test
    fun `Gjeldende eskaleringsvarsel som er 14 dager eller eldre skal sendes til oversikten-utboks`() {
        opprettEskaleringsvarselEldreEnn(ZonedDateTime.now().minusDays(14))
        eskaleringsvarselService.sendUtgåtteVarslerTilOversikten()
        assertThat(oversiktenUtboksRepository.hentAlleSomSkalSendes()).hasSize(1)
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