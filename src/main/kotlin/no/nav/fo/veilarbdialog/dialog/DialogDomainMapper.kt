package no.nav.fo.veilarbdialog.dialog

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.dialog.exceptions.FantIkkeDialogTrådException
import no.nav.fo.veilarbdialog.dialog.exceptions.UgyldigDialogInputException
import no.nav.fo.veilarbdialog.dialog.opprett.NyDialog
import no.nav.fo.veilarbdialog.dialog.opprett.NyDialogEllerMelding
import no.nav.fo.veilarbdialog.dialog.opprett.NyDialogFraBruker
import no.nav.fo.veilarbdialog.dialog.opprett.NyDialogFraVeileder
import no.nav.fo.veilarbdialog.dialog.opprett.NyEskaleringsVarselDialog
import no.nav.fo.veilarbdialog.dialog.opprett.NyMelding
import no.nav.fo.veilarbdialog.dialog.opprett.NyMeldingFraBruker
import no.nav.fo.veilarbdialog.dialog.opprett.NyMeldingFraVeileder
import no.nav.fo.veilarbdialog.domain.AvsenderType
import no.nav.fo.veilarbdialog.domain.NyMeldingDTO

object DialogDomainMapper {
    @JvmStatic
    fun tilNyMeldingEllerDialog(data: NyMeldingDTO, fantDialog: Boolean, fnr: Fnr, aktorId: AktorId, avsenderId: String, avsenderType: AvsenderType): NyDialogEllerMelding {
        if (data.dialogId != null && !fantDialog) throw FantIkkeDialogTrådException(data.dialogId)
        return if (data.dialogId != null && fantDialog) {
            when (avsenderType) {
                AvsenderType.BRUKER -> NyMeldingFraBruker(
                    tekst = data.tekst ?: throw UgyldigDialogInputException("Alle meldinger må ha overskrift"),
                    dialogId = runCatching { data.dialogId.toLong() }.getOrElse { throw UgyldigDialogInputException("Alle meldinger må ha en gyldig dialogId") },
                    fnr = fnr,
                    aktorId = aktorId,
                    avsenderId = avsenderId,
                )
                AvsenderType.VEILEDER -> NyMeldingFraVeileder(
                    tekst = data.tekst ?: throw UgyldigDialogInputException("Alle meldinger må ha overskrift"),
                    dialogId = runCatching { data.dialogId.toLong() }.getOrElse { throw UgyldigDialogInputException("Alle meldinger må ha en gyldig dialogId") },
                    fnr = fnr,
                    aktorId = aktorId,
                    avsenderId = avsenderId,
                )
            }
        } else {
            when (avsenderType) {
                AvsenderType.BRUKER -> NyDialogFraBruker(
                    tekst = data.tekst ?: throw UgyldigDialogInputException("Alle nye dialoger må ha tekst"),
                    fnr = fnr,
                    aktorId = aktorId,
                    overskrift = data.overskrift
                        ?: throw UgyldigDialogInputException("Alle nye dialoger må ha overskrift"),
                    aktivitetId = data.aktivitetId,
                    avsenderId = avsenderId,
                    venterPaaSvarFraNav = data.venterPaaSvarFraNav ?: true,
                )
                AvsenderType.VEILEDER -> NyDialogFraVeileder(
                    tekst = data.tekst ?: throw UgyldigDialogInputException("Alle nye dialoger må ha tekst"),
                    fnr = fnr,
                    aktorId = aktorId,
                    overskrift = data.overskrift
                        ?: throw UgyldigDialogInputException("Alle nye dialoger må ha overskrift"),
                    aktivitetId = data.aktivitetId,
                    // TODO: Verifiser at false er riktig default!!!!
                    venterPaaSvarFraBruker = data.venterPaaSvarFraBruker ?: false,
                    venterPaaSvarFraNav = data.venterPaaSvarFraNav ?: false,
                    avsenderId = avsenderId,
                )
            }
        }
    }

    @JvmStatic
    fun nyDialogTilNyMelding(data: NyDialog, dialogId: Long): NyMelding {
        return when (data) {
            is NyDialogFraBruker -> NyMeldingFraBruker(
                tekst = data.tekst,
                dialogId = dialogId,
                fnr = data.fnr,
                aktorId = data.aktorId,
                avsenderId = data.avsenderId,
            )
            is NyDialogFraVeileder -> NyMeldingFraVeileder(
                tekst = data.tekst,
                dialogId = dialogId,
                fnr = data.fnr,
                aktorId = data.aktorId,
                avsenderId = data.avsenderId,
            )
            is NyEskaleringsVarselDialog -> NyMeldingFraVeileder(
                tekst = data.tekst,
                dialogId = dialogId,
                fnr = data.fnr,
                aktorId = data.aktorId,
                avsenderId = data.avsenderId,
            )
        }
    }
}