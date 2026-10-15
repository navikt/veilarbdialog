package no.nav.fo.veilarbdialog.kvp

import no.nav.common.json.JsonUtils
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.domain.kafka.KvpAvsluttet
import no.nav.fo.veilarbdialog.domain.kafka.KvpAvsluttetKafkaDTO
import no.nav.fo.veilarbdialog.domain.kafka.KvpPeriode
import no.nav.fo.veilarbdialog.domain.kafka.KvpPeriodeEventType
import no.nav.fo.veilarbdialog.domain.kafka.KvpStartet
import org.apache.kafka.clients.consumer.Consumer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import java.time.ZonedDateTime

class KvpConsumerTest : SpringBootTestBase() {
    companion object {
        private const val AKTORID = "4321"
        private const val SAKSBEHANDLER = "Z99999"
        private const val BEGRUNNELSE = "Derfor"
        private val AVSLUTTETDATO = ZonedDateTime.now()
    }

    @Autowired
    lateinit var producer: KafkaTemplate<String, String>

    private lateinit var springKvpAvsluttetConsumer: Consumer<String, String>

    @Value("\${application.topic.inn.kvpavsluttet}")
    lateinit var kvpAvsluttetTopic: String

    @Value("\${application.topic.inn.kvpperioder}")
    lateinit var kvpPerioderTopic: String

    @BeforeEach
    fun setupConsumer() {
        springKvpAvsluttetConsumer = kafkaTestService.createStringStringConsumer(kvpAvsluttetTopic)
    }

    @Test
    fun behandleKvpAvsluttetConsumerService_spiser_meldinger_fra_kvpAvsluttetTopic() {
        val kvpAvsluttetKafkaDTO = KvpAvsluttetKafkaDTO.builder()
            .aktorId(AKTORID)
            .avsluttetAv(SAKSBEHANDLER)
            .avsluttetBegrunnelse(BEGRUNNELSE)
            .avsluttetDato(AVSLUTTETDATO)
            .build()

        val sendResult = producer.send(
            kvpAvsluttetTopic,
            kvpAvsluttetKafkaDTO.aktorId,
            JsonUtils.toJson(kvpAvsluttetKafkaDTO)
        ).get()

        kafkaTestService.assertErKonsumertAiven(
            kvpAvsluttetTopic,
            sendResult.recordMetadata.offset(),
            sendResult.recordMetadata.partition(),
            10
        )
    }

    @Test
    fun behandleKvpPeriodeConsumerService_spiser_meldinger_fra_kvpPeriodeTopic() {
        val avsluttetDato = ZonedDateTime.now().minusMinutes(1)
        val startet = KvpStartet(
            SAKSBEHANDLER,
            avsluttetDato.minusDays(5),
            BEGRUNNELSE
        )
        val avsluttet = KvpAvsluttet(
            SAKSBEHANDLER,
            avsluttetDato,
            BEGRUNNELSE
        )
        val kvpPeriode = KvpPeriode(
            KvpPeriodeEventType.AVSLUTTET,
            AKTORID,
            null,
            startet,
            avsluttet
        )

        val sendResult = producer.send(
            kvpPerioderTopic,
            AKTORID,
            JsonUtils.toJson(kvpPeriode)
        ).get()

        kafkaTestService.assertErKonsumertAiven(
            kvpPerioderTopic,
            sendResult.recordMetadata.offset(),
            sendResult.recordMetadata.partition(),
            10
        )
    }
}
