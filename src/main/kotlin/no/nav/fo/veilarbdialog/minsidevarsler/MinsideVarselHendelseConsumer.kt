package no.nav.fo.veilarbdialog.minsidevarsler

import no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering.KvitteringMetrikk
import no.nav.fo.veilarbdialog.minsidevarsler.dto.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class MinsideVarselHendelseConsumer(
    @Value("\${spring.application.name}")
    private val appname: String,
    private val minsideVarselService: MinsideVarselService,
    private val kvitteringMetrikk: KvitteringMetrikk,
) {

    private val log = LoggerFactory.getLogger(MinsideVarselHendelseConsumer::class.java)

    @Transactional
    @KafkaListener(topics = ["\${application.topic.inn.minside.varsel-hendelse}"], containerFactory = "stringStringKafkaListenerContainerFactory", autoStartup = "\${app.kafka.enabled:false}")
    open fun consume(kafkaRecord: ConsumerRecord<String, String> ) {
        val varselHendelse = kafkaRecord.value().deserialiserVarselHendelse(appname)
        when (varselHendelse) {
            is EksternVarselOppdatering -> behandleEksternVarselHendelse(varselHendelse)
            is InternVarselHendelseDTO -> behandleInternVarselHendelse(varselHendelse)
            VarselFraAnnenApp -> {}
        }
    }

    private fun behandleInternVarselHendelse(varsel: InternVarselHendelseDTO) {
        log.info("Minside varsel (intern) av type {} er {} varselId {}", varsel.varseltype, varsel.eventName, varsel.varselId)
        when (varsel.eventName) {
            InternVarselHendelseType.opprettet -> {}
            InternVarselHendelseType.inaktivert -> {
                // sett varselstatus ferdig
                minsideVarselService.setVarselstatusFerdigbehandlet(varsel.varselId)
               // minsideVarselService.setEksternVarselAvsluttet(varsel.varselId)
            }
            InternVarselHendelseType.slettet -> {}
        }
    }

    private fun behandleEksternVarselHendelse(varsel: EksternVarselOppdatering) {
        var varselId = varsel.varselId
        log.info("Konsumerer minside-varsel-hendelse varselId={}, type={}", varselId, varsel.hendelseType.name);

        if (!minsideVarselService.finnesBrukernotifikasjon(varselId)) {
            log.warn("Mottok kvittering for brukernotifikasjon varselId={} som ikke finnes i våre systemer", varselId)
            return
        }

        log.info("Minside varsel (ekstern) av type {} er {} varselId {}", varsel.varseltype, varsel.hendelseType, varselId);
        when (varsel) {
            is Bestilt -> {}
            is Feilet -> {
                log.error("varsel feilet for notifikasjon varselId={} med feilmelding {}", varselId, varsel.feilmelding);
                minsideVarselService.setEksternVarselFeilet(varselId)
            }
            is Renotifikasjon -> {
                log.info("Minside varsel renotifkasjon sendt i kanal {} for varselId={}", varsel.kanal.name, varselId)
            }
            is Sendt -> {
                minsideVarselService.setEksternVarselKvitteringStatusOk(varselId)
                log.info("Minside varsel sendt i kanal {} for varselId={}", varsel.kanal.name,  varselId)
            }
            is Venter -> {}
            is Kasellert, is Ferdigstilt -> {
                minsideVarselService.setEksternVarselKvitteringStatusOk(varselId)
            }
        }

        kvitteringMetrikk.incrementBrukernotifikasjonKvitteringMottatt(varsel.hendelseType);
    }
}
