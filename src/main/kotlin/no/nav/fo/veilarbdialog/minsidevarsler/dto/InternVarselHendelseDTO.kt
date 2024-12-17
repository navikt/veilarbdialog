package no.nav.fo.veilarbdialog.minsidevarsler.dto

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.tms.varsel.action.Varseltype

data class InternVarselHendelseDTO(
    @JsonProperty("@event_name") val eventName: InternVarselHendelseType,
    val namespace: String,
    val appnavn: String,
    val varseltype: Varseltype,
    val varselId: MinSideVarselId,
): VarselHendelse() {
    fun getHendelseType(): VarselHendelseEventType {
        return when (eventName) {
            InternVarselHendelseType.opprettet -> VarselHendelseEventType.opprettet
            InternVarselHendelseType.inaktivert -> VarselHendelseEventType.inaktivert
            InternVarselHendelseType.slettet -> VarselHendelseEventType.slettet
        }
    }
}

enum class InternVarselHendelseType {
    opprettet,
    inaktivert,
    slettet
}
