package no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.fo.veilarbdialog.minsidevarsler.dto.EksternVarselKanal
import no.nav.fo.veilarbdialog.minsidevarsler.dto.EksternVarselStatus
import no.nav.tms.varsel.action.Varseltype
import java.util.*

/* Kun brukt i tester foreløpig */
data class TestVarselHendelseDTO(
    @JsonProperty("@event_name")
    val eventName: String,
    val namespace: String,
    val appnavn: String,
    val varseltype: Varseltype,
    val varselId: UUID,
    val status: EksternVarselStatus?,
    val renotifikasjon: Boolean? = null, // Også kalt "revarsling"
    val feilmelding: String? = null,
    val kanal: EksternVarselKanal? = null
)