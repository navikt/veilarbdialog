package no.nav.fo.veilarbdialog.minsidevarsler.dto

import no.nav.common.json.JsonUtils
import no.nav.tms.varsel.action.Varseltype
import java.util.*
import kotlin.text.replaceFirstChar
import kotlin.text.titlecase

/* Alle typer hendelser, inkl ekstern  */
enum class VarselHendelseEventType {
    opprettet,
    inaktivert,
    slettet,
    sendt_ekstern,
    renotifikasjon_ekstern,
    bestilt_ekstern,
    feilet_ekstern
}

const val EksternStatusOppdatertEventName = "eksternStatusOppdatert"

fun String.deserialiserVarselHendelse(appName: String): VarselHendelse {
    val jsonTree = JsonUtils.getMapper().readTree(this)
    val eventName = jsonTree["@event_name"].asText()
    val appNavn = jsonTree["appnavn"].asText()
    val varseltype = Varseltype.valueOf(jsonTree["varseltype"].asText().replaceFirstChar { it.titlecase()})
    if (appNavn != appName) return VarselFraAnnenApp
    return when (eventName == EksternStatusOppdatertEventName) {
        true -> jsonTree.deserialiserEksternVarselHendelse()
        else -> {
            return InternVarselHendelseDTO(
                namespace = jsonTree["namespace"].asText(),
                varseltype = varseltype,
                appnavn = appNavn,
                varselId = MinSideVarselId(UUID.fromString(jsonTree["varselId"].asText())),
                eventName = InternVarselHendelseType.valueOf(eventName)
            )
        }
    }
}