package no.nav.fo.veilarbdialog.eskaleringsvarsel

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.domain.NyMeldingDTO
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EskaleringsvarselServiceTest: SpringBootTestBase() {

    private val veileder = MockNavService.createVeileder()
    private val bruker = MockNavService.createHappyBruker()

    @Test
    fun `Gjeldende eskaleringsvarsel eldre enn 14 dager skal sendes til oversikten-utboks`() {
        val eskaleringsvarsel = eskaleringsvarsel()
        assertThat(eskaleringsvarselRepository.hentGjeldende(AktorId.of(bruker.aktorId))).isPresent
    }


    fun eskaleringsvarsel() {
        val dialog = dialogTestService.opprettDialogSomVeileder(veileder, bruker, NyMeldingDTO().setTekst("").setOverskrift(""))
        val brukernotifikasjon = dialogTestService.startEskalering(veileder, StartEskaleringDto(Fnr.of(bruker.fnr),"", "", "", "" ))
//        return EskaleringsvarselEntity(
//            varselId = 123,
//            tilhorendeDialogId = dialog.id.toLong(),
//            tilhorendeBrukernotifikasjonId = brukernotifikasjon.id,
//            tilhorendeVarselId = null,
//            aktorId = bruker.aktorId,
//            opprettetAv = null,
//            opprettetDato = null,
//            opprettetBegrunnelse = null,
//            avsluttetDato = null,
//            avsluttetAv = null,
//            avsluttetBegrunnelse = null,
//        )
    }
}