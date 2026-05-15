package no.nav.fo.veilarbdialog.dialog.opprett

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.domain.AvsenderType

data class NyDialogFraNav(
    override val tekst: String,
    override val overskrift: String,
    override val aktivitetId: String?,
    val venterPaaSvarFraNav: Boolean,
    val venterPaaSvarFraBruker: Boolean,
    override val fnr: Fnr,
    override val aktorId: AktorId,
    override val avsenderId: String,
): NyDialog {
    override val avsenderType: AvsenderType
        get() = AvsenderType.VEILEDER
}