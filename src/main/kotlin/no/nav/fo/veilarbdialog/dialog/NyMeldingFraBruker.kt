package no.nav.fo.veilarbdialog.dialog

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.domain.AvsenderType

class NyMeldingFraBruker(
    override val tekst: String,
    override val dialogId: Long,
    override val fnr: Fnr,
    override val aktorId: AktorId,
    override val avsenderId: String,
): NyMelding {
    override val avsenderType: AvsenderType
        get() = AvsenderType.BRUKER
}

