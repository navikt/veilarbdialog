package no.nav.fo.veilarbdialog.kvp

import io.micrometer.core.annotation.Timed
import no.nav.common.json.JsonUtils
import no.nav.fo.veilarbdialog.domain.kafka.KvpPeriode
import no.nav.fo.veilarbdialog.domain.kafka.KvpPeriodeEventType
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class KvpPeriodeConsumer(
    private val kvpAvsluttetHendelseService: KvpAvsluttetHendelseService,
) {
    private val log = LoggerFactory.getLogger(KvpPeriodeConsumer::class.java)

    @KafkaListener(topics = ["\${application.topic.inn.kvpperioder}"], containerFactory = "stringStringKafkaListenerContainerFactoryLatestOffset", autoStartup = "\${app.kafka.enabled:false}")
    @Timed
    fun behandleKvpPeriode(consumerRecord: ConsumerRecord<String, String>) {
        val kvpPeriode = JsonUtils.fromJson(consumerRecord.value(), KvpPeriode::class.java)

        when (kvpPeriode.event) {
            KvpPeriodeEventType.STARTET -> log.info("Mottok KVP STARTET fra {}, ingen handling i veilarbdialog", consumerRecord.topic())
            KvpPeriodeEventType.AVSLUTTET -> kvpAvsluttetHendelseService.behandleAvsluttet(
                kvpPeriode.aktorId,
                hentAvsluttetDato(kvpPeriode),
            )
        }
    }

    private fun hentAvsluttetDato(kvpPeriode: KvpPeriode): ZonedDateTime {
        return requireNotNull(kvpPeriode.avsluttet?.avsluttetDato) { "avsluttet.avsluttetDato kan ikke være null for AVSLUTTET-event" }
    }
}
