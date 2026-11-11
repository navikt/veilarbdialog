package no.nav.fo.veilarbdialog.sisteDialogHendelse

import no.nav.fo.veilarbdialog.domain.DialogData
import java.time.ZoneId
import java.time.ZonedDateTime

data class SisteMelding(
    val dialogId: Long,
    val tidspunkt: ZonedDateTime,
)

data class SisteDialogHendelse(
    val sisteMeldingVeileder: SisteMelding?,
    val sisteMeldingBruker: SisteMelding?,
) {
    companion object {
        fun fromDialogMeldinger(dialoger: List<DialogData?>): SisteDialogHendelse {
            val (brukerMeldinger, veilederMeldinger) = dialoger
                .filterNotNull()
                .filterNot { it.isHistorisk }
                .flatMap { it.henvendelser ?: emptyList() }
                .partition { it.fraBruker() }

            return SisteDialogHendelse(
                sisteMeldingVeileder = veilederMeldinger
                    .maxByOrNull { it.sendt }
                    ?.let { SisteMelding(it.dialogId, it.sendt.toInstant().atZone(ZoneId.of("Europe/Oslo"))) },
                sisteMeldingBruker = brukerMeldinger
                    .maxByOrNull { it.sendt }
                    ?.let { SisteMelding(it.dialogId, it.sendt.toInstant().atZone(ZoneId.of("Europe/Oslo"))) },
            )
        }
    }
}
