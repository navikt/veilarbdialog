package no.nav.fo.veilarbdialog.minsidevarsler

import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonRepository
import no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering.KvitteringMetrikk
import no.nav.fo.veilarbdialog.minsidevarsler.dto.Bestilt
import no.nav.fo.veilarbdialog.minsidevarsler.dto.EksternVarsling
import no.nav.fo.veilarbdialog.minsidevarsler.dto.Feilet
import no.nav.fo.veilarbdialog.minsidevarsler.dto.InternVarselHendelseDTO
import no.nav.fo.veilarbdialog.minsidevarsler.dto.Renotifikasjon
import no.nav.fo.veilarbdialog.minsidevarsler.dto.Sendt
import no.nav.fo.veilarbdialog.minsidevarsler.dto.VarselFraAnnenApp
import no.nav.fo.veilarbdialog.minsidevarsler.dto.deserialiserVarselHendelse
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
//    private val kvitteringDAO: KvitteringDAO,
    private val brukernotifikasjonRepository: BrukernotifikasjonRepository,
    private val kvitteringMetrikk: KvitteringMetrikk,
) {

    private val log = LoggerFactory.getLogger(MinsideVarselHendelseConsumer::class.java)

    @Transactional
    @KafkaListener(topics = ["\${application.topic.inn.minside.varsel-hendelse}"], containerFactory = "stringStringKafkaListenerContainerFactory", autoStartup = "\${app.kafka.enabled:false}")
    open fun consume(kafkaRecord: ConsumerRecord<String, String> ) {
        val varselHendelse = kafkaRecord.value().deserialiserVarselHendelse(appname)
        when (varselHendelse) {
            is EksternVarsling -> behandleEksternVarsel(varselHendelse)
            is InternVarselHendelseDTO -> {}
            VarselFraAnnenApp -> {}
        }
    }

    private fun behandleEksternVarsel(varsel: EksternVarsling) {
        var varselId = varsel.varselId
        log.info("Konsumerer minside-varsel-hendelse varselId={}, type={}", varselId, varsel.hendelseType.name);

        if (!brukernotifikasjonRepository.finnesBrukernotifikasjon(varselId)) {
            log.warn("Mottok kvittering for brukernotifikasjon varselId={} som ikke finnes i våre systemer", varselId);
            throw IllegalArgumentException("Ugyldig varselId.")
        }

        // TODO: Vurder om denne trengs
//        kvitteringDAO.lagreKvittering(varselId, melding)

        when (varsel) {
            is Bestilt -> {}
            is Feilet -> {
                log.error("varsel feilet for notifikasjon varselId={} med feilmelding {}", varselId, varsel.feilmelding);
                brukernotifikasjonRepository.setEksternVarselFeilet(varselId);
            }
            is Renotifikasjon -> {}
            is Sendt -> {
                brukernotifikasjonRepository.setEksternVarselSendtOk(varselId);
                log.info("Varsel fullført for varselId={}", varselId);
            }
        }

//        List<Kvittering> kvitterings = kvitteringDAO.hentKvitteringer(bestillingsId);
//        log.info("EksternVarsel Kvitteringshistorikk {}", kvitterings);

        kvitteringMetrikk.incrementBrukernotifikasjonKvitteringMottatt(varsel.hendelseType);
    }
}
