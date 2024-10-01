package no.nav.fo.veilarbdialog.manuellStatus

import no.nav.common.json.JsonUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class ManuellStatusConsumer(
    val manuellStatusDao: ManuellStatusDao
) {
    private val logger = LoggerFactory.getLogger(ManuellStatusConsumer::class.java)

    @KafkaListener(
        topics = ["\${application.topic.inn.manuellStatus}"],
        containerFactory = "stringStringKafkaListenerContainerFactory",
        autoStartup = "\${app.kafka.enabled:false}"
    )
    fun oppdaterManuellStatus(consumerRecord: ConsumerRecord<String, String> ) {
        val manuellStatus = JsonUtils.fromJson(consumerRecord.value(), ManuellStatusDto::class.java)
        runCatching {
            manuellStatusDao.upsertManuellStatus(manuellStatus)
        }.onFailure { logger.warn("Kunne ikke oppdatere manuell status", it) }
    }

}

