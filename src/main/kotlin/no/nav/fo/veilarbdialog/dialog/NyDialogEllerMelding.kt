package no.nav.fo.veilarbdialog.dialog

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.domain.AvsenderType


sealed interface NyDialogEllerMelding {
    val tekst: String
    val avsenderId: String
    val avsenderType: AvsenderType
}

sealed interface NyDialog: NyDialogEllerMelding {
    val overskrift: String
    val aktivitetId: String?
    override val tekst: String
    val fnr: Fnr
    val aktorId: AktorId
}

sealed interface NyMelding: NyDialogEllerMelding {
    override val tekst: String
    val dialogId: Long
    val fnr: Fnr
    val aktorId: AktorId
}
