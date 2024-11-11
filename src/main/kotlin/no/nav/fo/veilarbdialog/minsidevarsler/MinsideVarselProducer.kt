package no.nav.fo.veilarbdialog.minsidevarsler

import lombok.`var`
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType.BESKJED
import no.nav.fo.veilarbdialog.domain.Person.Fnr
import no.nav.tms.varsel.action.EksternKanal
import no.nav.tms.varsel.action.Produsent
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.net.URL
import java.time.ZonedDateTime
import java.util.*

data class PendingVarsel(
    val varselId: UUID,
    val melding: String,
//brukernotifikasjonEntity.oppfolgingsPeriodeId().toString(),
    val epostTittel: String,
    val epostBody: String,
    val smsText: String,
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

    open fun publiserVarselPåKafka(varsel: PendingVarsel) {
        val melding = VarselActionBuilder.opprett {
            varselId = varsel.varselId.toString()
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
                smsVarslingstekst = varsel.smsText
                epostVarslingstittel = varsel.epostTittel
                epostVarslingstekst = varsel.epostBody
                utsettSendingTil = ZonedDateTime.now().plusMinutes(5)
            }
            produsent = Produsent(
                cluster = cluster,
                namespace = namespace,
                appnavn = applicationName
            )
        }
        kafkaTemplate.send(topic, varsel.varselId.toString(), melding)
    }

}
