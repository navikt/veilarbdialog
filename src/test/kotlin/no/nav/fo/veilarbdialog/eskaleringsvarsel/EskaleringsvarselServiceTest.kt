package no.nav.fo.veilarbdialog.eskaleringsvarsel

import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.domain.NyMeldingDTO
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService
import no.nav.veilarbaktivitet.veilarbdbutil.VeilarbDialogSqlParameterSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class EskaleringsvarselServiceTest: SpringBootTestBase() {

    private val bruker = MockNavService.createHappyBruker()
    private val veileder = MockNavService.createVeileder(bruker)

    @Test
    fun `Gjeldende eskaleringsvarsel eldre enn 14 dager skal sendes til oversikten-utboks`() {
        opprettEskaleringsvarselEldreEnn(ZonedDateTime.now().minusDays(15))

        eskaleringsvarselService.sendUtgåtteVarslerTilOversikten()

        assertThat(oversiktenUtboksRepository.hentAlleSomSkalSendes()).hasSize(1)
    }


    fun opprettEskaleringsvarselEldreEnn(tidspunkt: ZonedDateTime) {
        val dialog = dialogTestService.opprettDialogSomVeileder(veileder, bruker, NyMeldingDTO().setTekst("").setOverskrift(""))
        val brukernotifikasjon = dialogTestService.startEskalering(veileder, StartEskaleringDto(Fnr.of(bruker.fnr),"", "", "", "" ))

        val sql = """
            UPDATE eskaleringsvarsel
            SET opprettet_dato = :dato
            WHERE gjeldende = :aktorId
        """.trimIndent()
        val params = VeilarbDialogSqlParameterSource()
            .addValue("dato", tidspunkt)
            .addValue("aktorId", bruker.aktorId)
        namedParameterJdbcTemplate.update(sql, params)
    }
}