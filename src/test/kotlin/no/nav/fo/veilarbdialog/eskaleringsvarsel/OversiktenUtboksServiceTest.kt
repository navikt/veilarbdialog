package no.nav.fo.veilarbdialog.eskaleringsvarsel

import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService
import no.nav.fo.veilarbdialog.oversiktenVaas.OversiktenUtboksRepository
import no.nav.fo.veilarbdialog.oversiktenVaas.OversiktenUtboksService
import no.nav.fo.veilarbdialog.oversiktenVaas.OversiktenUtboksService.Kategori.UTGATT_VARSEL
import no.nav.fo.veilarbdialog.oversiktenVaas.SendingEntity
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

open class OversiktenUtboksServiceTest: SpringBootTestBase() {

    @Autowired
    private lateinit var oversiktenUtboksRepository: OversiktenUtboksRepository

    private val bruker = MockNavService.createHappyBruker()

    @Test
    fun ``() {
        val melding = SendingEntity(
            fnr = Fnr.of(bruker.fnr),
            meldingSomJson = "{}",
            kategori = UTGATT_VARSEL,
            meldingKey = UUID.randomUUID(),

        )
    }

}