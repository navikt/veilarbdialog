package no.nav.fo.veilarbdialog.oversiktenVaas

import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.util.*

open class OversiktenServiceTest: SpringBootTestBase() {

    @Autowired
    private lateinit var oversiktenService: OversiktenService

    @Value("\${application.topic.ut.oversikten}")
    private lateinit var oversiktenTopic: String

    private val bruker = MockNavService.createHappyBruker()

    @Test
    fun `Skal sende usendte meldinger`() {
        val consumer = kafkaTestService.createStringStringConsumer(oversiktenTopic)
        val melding = melding(bruker)
        oversiktenUtboksRepository.lagreSending(melding)

        oversiktenService.sendUsendteMeldingerTilOversikten()

        kafkaTestService.assertHasNewRecord(oversiktenTopic, consumer)
        assertThat(oversiktenUtboksRepository.hentAlleSomSkalSendes()).isEmpty()
    }

    private fun melding(bruker: MockBruker) =
        SendingEntity(
            fnr = Fnr.of(bruker.fnr),
            meldingSomJson = "{}",
            kategori = OversiktenMelding.Kategori.UTGATT_VARSEL,
            meldingKey = UUID.randomUUID(),
        )

}