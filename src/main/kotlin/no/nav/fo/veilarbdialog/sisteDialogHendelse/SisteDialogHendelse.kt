package no.nav.fo.veilarbdialog.sisteDialogHendelse

import no.nav.fo.veilarbdialog.domain.DialogData
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

data class SisteMelding(
    val dialogId: Long,
    val tidspunkt: ZonedDateTime,
)

data class SisteDialogHendelse(
    val sisteMeldingVeileder: SisteMelding?,
    val sisteMeldingBruker: SisteMelding?,
) {
    companion object {
        fun fromDialogMeldinger(dialoger: List<DialogData?>, aktivPeriodeId: UUID?): SisteDialogHendelse? {

            if (aktivPeriodeId == null) return null

            val (brukerMeldinger, veilederMeldinger) = dialoger
                .filterNotNull()
                .filter { it.oppfolgingsperiode == aktivPeriodeId }
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
