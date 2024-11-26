package no.nav.fo.veilarbdialog.eskaleringsvarsel

import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService
import no.nav.fo.veilarbdialog.oversiktenVaas.OversiktenUtboksRepository
import no.nav.fo.veilarbdialog.oversiktenVaas.OversiktenUtboksService
import no.nav.fo.veilarbdialog.oversiktenVaas.OversiktenUtboksService.Kategori.UTGATT_VARSEL
import no.nav.fo.veilarbdialog.oversiktenVaas.SendingEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.util.*

open class OversiktenUtboksServiceTest: SpringBootTestBase() {

    @Autowired
    private lateinit var oversiktenUtboksRepository: OversiktenUtboksRepository

    @Autowired
    private lateinit var oversiktenUtboksService: OversiktenUtboksService

    @Value("\${application.topic.ut.oversikten}")
    private lateinit var oversiktenTopic: String

    private val bruker = MockNavService.createHappyBruker()

    @Test
    fun `Skal sende usendte meldinger`() {
        val consumer = kafkaTestService.createStringStringConsumer(oversiktenTopic)
        val melding = melding(bruker)
        oversiktenUtboksRepository.lagreSending(melding)

        oversiktenUtboksService.sendUsendteMeldingerTilOversikten()

        kafkaTestService.assertHasNewRecord(oversiktenTopic, consumer)
        assertThat(oversiktenUtboksRepository.hentAlleSomSkalSendes()).isEmpty()
    }

    private fun melding(bruker: MockBruker) =
        SendingEntity(
            fnr = Fnr.of(bruker.fnr),
            meldingSomJson = "{}",
            kategori = UTGATT_VARSEL,
            meldingKey = UUID.randomUUID(),
        )

}