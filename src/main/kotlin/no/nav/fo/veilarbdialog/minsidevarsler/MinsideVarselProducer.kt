package no.nav.fo.veilarbdialog.minsidevarsler

import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType
import no.nav.tms.varsel.builder.VarselActionBuilder
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.net.URL
import java.util.UUID

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
    val kafkaTemplate: KafkaTemplate<String, String>
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

            }
        }
//        kafkaTemplate.send()
    }

}