package no.nav.fo.veilarbdialog.minsidevarsler

import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType.BESKJED
import no.nav.tms.varsel.action.EksternKanal
import no.nav.tms.varsel.action.Produsent
import no.nav.tms.varsel.builder.VarselActionBuilder
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
    val type: BrukernotifikasjonsType
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
) {

    open fun publiserVarselPåKafka(varsel: PendingVarsel) {
        /*
        new BeskjedInputBuilder()
            .withTidspunkt(LocalDateTime.now(ZoneOffset.UTC))
            .withTekst(beskjedInfo.getMelding())
            .withLink(beskjedInfo.getLink())
            .withSikkerhetsnivaa(BESKJED_SIKKERHETSNIVAA)
            .withEksternVarsling(true)
            .withPrefererteKanaler(PreferertKanal.SMS)
            .withSmsVarslingstekst(beskjedInfo.getSmsTekst())
            .withEpostVarslingstittel(beskjedInfo.getEpostTitel())
            .withEpostVarslingstekst(beskjedInfo.getEpostBody())
            .withSynligFremTil(LocalDateTime.now(ZoneOffset.UTC).plusMonths(1))
            .build();
         */
        VarselActionBuilder.opprett {
            varselId = varsel.varselId.toString()
            eksternVarsling {
                preferertKanal = EksternKanal.SMS
                smsVarslingstekst = varsel.smsText
                epostVarslingstittel = varsel.epostTittel
                epostVarslingstekst = varsel.epostBody
                kanBatches = if (varsel.type == BESKJED) true else false
                utsettSendingTil = ZonedDateTime.now().plusMinutes(5)
            }
            produsent = Produsent(
                cluster = cluster,
                namespace = namespace,
                appnavn = applicationName
            )
        }
//        kafkaTemplate.send()
    }

}
