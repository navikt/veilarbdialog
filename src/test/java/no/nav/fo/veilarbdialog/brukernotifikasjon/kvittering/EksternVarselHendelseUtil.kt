package no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering

import no.nav.fo.veilarbdialog.minsidevarsler.dto.EksternStatusOppdatertEventName
import no.nav.fo.veilarbdialog.minsidevarsler.dto.EksternVarselHendelseDTO
import no.nav.fo.veilarbdialog.minsidevarsler.dto.EksternVarselKanal
import no.nav.fo.veilarbdialog.minsidevarsler.dto.EksternVarselStatus
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinSideVarselId
import no.nav.tms.varsel.action.Varseltype

object EksternVarselHendelseUtil {
    fun lagVarselHendelseMelding(varselId: MinSideVarselId, status: EksternVarselStatus, appname: String): EksternVarselHendelseDTO {
        return EksternVarselHendelseDTO(
            EksternStatusOppdatertEventName,
            "dab",
            appname,
            Varseltype.Beskjed,
            varselId.value,
            status,
            false,
            null,
            EksternVarselKanal.SMS
        )
    }

    fun eksternVarselHendelseSendt(bestillingsId: MinSideVarselId, appname: String): EksternVarselHendelseDTO {
        return lagVarselHendelseMelding(bestillingsId, EksternVarselStatus.sendt, appname)
    }

    fun eksternVarselHendelseFeilet(bestillingsId: MinSideVarselId, appname: String): EksternVarselHendelseDTO {
        return lagVarselHendelseMelding(bestillingsId, EksternVarselStatus.feilet, appname)
    }

    fun eksternVarselHendelseBestilt(eventId: MinSideVarselId, appname: String): EksternVarselHendelseDTO {
        return lagVarselHendelseMelding(eventId, EksternVarselStatus.bestilt, appname)
    }
}