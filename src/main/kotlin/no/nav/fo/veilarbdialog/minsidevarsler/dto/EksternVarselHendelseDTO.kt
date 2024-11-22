package no.nav.fo.veilarbdialog.minsidevarsler.dto

import com.fasterxml.jackson.databind.JsonNode
import no.nav.tms.varsel.action.Varseltype
import java.util.*

sealed class VarselHendelse

object VarselFraAnnenApp: VarselHendelse()
sealed class EksternVarselOppdatering(
    val varselId: MinSideVarselId,
    val hendelseType: VarselHendelseEventType,
    val varseltype: Varseltype
): VarselHendelse()

class Renotifikasjon(
    varseltype: Varseltype,
    varselId: MinSideVarselId,
    val kanal: EksternVarselKanal
): EksternVarselOppdatering(varselId, VarselHendelseEventType.renotifikasjon_ekstern, varseltype)
class Sendt(
    varseltype: Varseltype,
    varselId: MinSideVarselId,
    val kanal: EksternVarselKanal
): EksternVarselOppdatering(varselId, VarselHendelseEventType.sendt_ekstern, varseltype)
class Bestilt(
    varseltype: Varseltype,
    varselId: MinSideVarselId,
): EksternVarselOppdatering(varselId, VarselHendelseEventType.bestilt_ekstern, varseltype)
class Feilet(
    varseltype: Varseltype,
    varselId: MinSideVarselId,
    val feilmelding: String
): EksternVarselOppdatering(varselId, VarselHendelseEventType.feilet_ekstern, varseltype)
class Venter(
    varseltype: Varseltype,
    varselId: MinSideVarselId,
): EksternVarselOppdatering(varselId, VarselHendelseEventType.venter_ekstern, varseltype)
class Kasellert(
    varseltype: Varseltype,
    varselId: MinSideVarselId,
): EksternVarselOppdatering(varselId, VarselHendelseEventType.kansellert_ekstern, varseltype)
class Ferdigstilt(
    varseltype: Varseltype,
    varselId: MinSideVarselId,
): EksternVarselOppdatering(varselId, VarselHendelseEventType.ferdigstilt_ekstern, varseltype)


fun JsonNode.deserialiserEksternVarselHendelse(): EksternVarselOppdatering {
    val eksternStatus = EksternVarselStatus.valueOf(this["status"].asText())
    val varselId = MinSideVarselId(UUID.fromString(this["varselId"].asText()))
    val varseltype = Varseltype.valueOf(this["varseltype"].asText().replaceFirstChar { it.titlecase()})
    return when (eksternStatus) {
        EksternVarselStatus.sendt -> {
            val kanal = EksternVarselKanal.valueOf(this["kanal"].asText())
            when (this["renotifikasjon"].asBoolean()) {
                true -> Renotifikasjon(varseltype, varselId, kanal)
                else -> {
                    Sendt(varseltype, varselId, kanal)
                }
            }
        }
        EksternVarselStatus.bestilt -> {
            Bestilt(varseltype, varselId)
        }
        EksternVarselStatus.feilet -> {
            Feilet(
                varseltype,
                varselId,
                this["feilmelding"].asText()
            )
        }
        EksternVarselStatus.venter -> Venter(varseltype, varselId)
        EksternVarselStatus.kansellert -> Kasellert(varseltype, varselId)
        EksternVarselStatus.ferdigstilt -> Ferdigstilt(varseltype, varselId)
    }
}

enum class EksternVarselKanal {
    SMS,
    EPOST
}

enum class EksternVarselStatus {
    bestilt,
    sendt,
    feilet,
    venter, // Bestilling av varsling på sms/epost er lagt til i intern kø
    kansellert, // Det underliggende varselet ble inaktivert før bestilling ble oversendt til altinn
    ferdigstilt // alle eksterne varsler er ferdig
}
