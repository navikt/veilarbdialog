package no.nav.fo.veilarbdialog.minsidevarsler

import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType.BESKJED
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinSideVarselId
import no.nav.tms.varsel.action.*
import no.nav.tms.varsel.builder.VarselActionBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.net.URL
import java.time.ZonedDateTime

data class PendingVarsel(
    val varselId: MinSideVarselId,
    val melding: String,
    val lenke: URL,
    val type: BrukernotifikasjonsType,
    val fnr: Fnr
)

@Service
open class MinsideVarselProducer(
    val kafkaTemplate: KafkaTemplate<String, String>,
    @Value("\${application.namespace}")
    private val namespace: String,
    @Value("\${application.cluster}")
    private val cluster: String,
    @Value("\${spring.application.name}")
    private val applicationName: String,
    @Value("\${application.topic.ut.minside.varsel}")
    private val topic: String,
) {
    private val log = LoggerFactory.getLogger(MinsideVarselProducer::class.java)

    open fun publiserVarselPåKafka(varsel: PendingVarsel) {
        val melding = VarselActionBuilder.opprett {
            varselId = varsel.varselId.value.toString()
            type = if (varsel.type == BESKJED) Varseltype.Beskjed else Varseltype.Oppgave
            ident = varsel.fnr.get()
            sensitivitet = if (varsel.type == BESKJED) Sensitivitet.Substantial else Sensitivitet.High
            link = varsel.lenke.toString()
            aktivFremTil = ZonedDateTime.now().plusMonths(1)
            tekst = Tekst(
                spraakkode = "nb",
                tekst = varsel.melding,
                default = true
            )
            eksternVarsling {
                preferertKanal = EksternKanal.SMS
                smsVarslingstekst = null
                epostVarslingstittel = null
                epostVarslingstekst = null
                utsettSendingTil = ZonedDateTime.now().plusMinutes(5)
            }
            produsent = Produsent(
                cluster = cluster,
                namespace = namespace,
                appnavn = applicationName
            )
        }
        kafkaTemplate.send(topic, varsel.varselId.value.toString(), melding)
    }

    open fun publiserInaktiveringsMeldingPåKafka(inativerVarsel: VarselInaktivering) {
        val melding = VarselActionBuilder.inaktiver {
            this.varselId = inativerVarsel.varselId.value.toString()
            this.produsent = Produsent(
                cluster = cluster,
                namespace = namespace,
                appnavn = applicationName
            )
        }
        kafkaTemplate.send(topic, inativerVarsel.varselId.value.toString(), melding)
        log.info("Sendt done for varselId: {}", inativerVarsel.varselId);
    }

}
