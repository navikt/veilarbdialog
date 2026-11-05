package no.nav.fo.veilarbdialog.sisteDialogHendelse

import java.time.ZonedDateTime

data class SisteMelding(
    val dialogId: Long,
    val tidspunkt: ZonedDateTime,
)

data class SisteDialogHendelse(
    val sisteMeldingVeileder: SisteMelding?,
    val sisteMeldingBruker: SisteMelding?,
)
