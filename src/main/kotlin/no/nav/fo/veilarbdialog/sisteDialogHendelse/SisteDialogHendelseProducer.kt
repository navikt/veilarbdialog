package no.nav.fo.veilarbdialog.sisteDialogHendelse

import no.nav.common.json.JsonUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
open class SisteDialogHendelseProducer(
    val kafkaTemplate: KafkaTemplate<String, String>,
    @Value("\${application.topic.ut.sisteDialogHendelse}")
    private val topic: String,
) {
    private val log = LoggerFactory.getLogger(SisteDialogHendelseProducer::class.java)

    open fun sendSisteDialogHendelse(aktorId: String, hendelse: SisteDialogHendelse) {
        kafkaTemplate.send(topic, aktorId, JsonUtils.toJson(hendelse))
        log.info("Siste dialog-hendelse publisert for dialogId: {}, {}", hendelse.sisteMeldingBruker?.dialogId, hendelse.sisteMeldingVeileder?.dialogId)
    }
}
