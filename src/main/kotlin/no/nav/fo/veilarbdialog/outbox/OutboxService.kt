package no.nav.fo.veilarbdialog.outbox

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class OutboxService(
    private val outboxDao: OutboxDao,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(OutboxService::class.java)

    @Transactional(propagation = Propagation.MANDATORY)
    open fun lagreIOutbox(topic: String, key: String, payload: String) {
        outboxDao.lagre(topic, key, payload)
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 60000)
    @SchedulerLock(name = "outbox_kafka_scheduledTask", lockAtMostFor = "PT2M")
    open fun sendUsendteMeldinger() {
        val meldinger = outboxDao.hentUsendteMeldinger()
        meldinger.forEach { melding ->
            kafkaTemplate.send(melding.topic, melding.key, melding.payload).get()
            outboxDao.slett(melding.id)
            log.info("Outbox-melding sendt og slettet: id={}, topic={}", melding.id, melding.topic)
        }
    }
}
