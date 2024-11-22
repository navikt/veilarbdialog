package no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering

import no.nav.fo.veilarbdialog.minsidevarsler.dto.*
import no.nav.fo.veilarbdialog.minsidevarsler.dto.InternVarselHendelseType.*
import no.nav.tms.varsel.action.Varseltype

object VarselHendelseUtil {
    fun lagEksternVarselHendelseMelding(varselId: MinSideVarselId, status: EksternVarselStatus, appname: String): TestVarselHendelseDTO {
        return TestVarselHendelseDTO(
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

    fun lagInternVarselHendelseMelding(varselId: MinSideVarselId, internVarselHendelseType: InternVarselHendelseType,  appname: String): TestVarselHendelseDTO {
        return TestVarselHendelseDTO(
            internVarselHendelseType.name,
            "dab",
            appname,
            Varseltype.Beskjed,
            varselId.value,
            null,
            false,
            null,
            null
        )
    }

    fun internVaselHendelseOpprettet(varselId: MinSideVarselId, appname: String): TestVarselHendelseDTO {
        return lagInternVarselHendelseMelding(varselId, opprettet, appname)
    }
    fun internVaselHendelseInaktivert(varselId: MinSideVarselId, appname: String): TestVarselHendelseDTO {
        return lagInternVarselHendelseMelding(varselId, inaktivert, appname)
    }
    fun internVaselHendelseSlettet(varselId: MinSideVarselId, appname: String) {
        lagInternVarselHendelseMelding(varselId, slettet, appname )
    }

    fun eksternVarselHendelseSendt(bestillingsId: MinSideVarselId, appname: String): TestVarselHendelseDTO {
        return lagEksternVarselHendelseMelding(bestillingsId, EksternVarselStatus.sendt, appname)
    }

    fun eksternVarselHendelseFeilet(bestillingsId: MinSideVarselId, appname: String): TestVarselHendelseDTO {
        return lagEksternVarselHendelseMelding(bestillingsId, EksternVarselStatus.feilet, appname)
    }

    fun eksternVarselHendelseBestilt(eventId: MinSideVarselId, appname: String): TestVarselHendelseDTO {
        return lagEksternVarselHendelseMelding(eventId, EksternVarselStatus.bestilt, appname)
    }
}