package no.nav.fo.veilarbdialog.domain

import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

data class KafkaDialogEndringMelding(
    val aktorId: String,
    /**
     * Venter på svar fra bruker.
     */
    private val tidspunktEldsteVentende: LocalDateTime? = null,
    /**
     * Venter på svar fra Nav.
     */
    private val tidspunktEldsteUbehandlede: LocalDateTime? = null
) {

    companion object {
        fun mapTilDialogData(_dialoger: List<DialogData?>, aktorId: String): KafkaDialogEndringMelding {
            val ikkeHistoriskeDialoger = _dialoger
                .filterNotNull()
                .filter { dialog -> !dialog.isHistorisk }

            return KafkaDialogEndringMelding(
                // Venter på svar fra bruker.
                // Oversikten bryr seg bare om denne har en dato eller er null
                tidspunktEldsteVentende = (
                    ikkeHistoriskeDialoger
                        .filter { it.venterPaSvarFraBruker() }
                        .mapNotNull { it.venterPaSvarFraBrukerSiden }
                        .minWithOrNull(Comparator.naturalOrder())
                        ?.let { LocalDateTime.ofInstant(it.toInstant(), ZoneId.systemDefault()) }
                ),
                // Venter på svar fra nav.
                // Oversikten bryr seg bare om denne har en dato eller er null
                tidspunktEldsteUbehandlede = (
                    ikkeHistoriskeDialoger
                        .filter { it.erUbehandlet() }
                        .mapNotNull { it.venterPaNavSiden }
                        .minWithOrNull(Comparator.naturalOrder())
                        ?.let { LocalDateTime.ofInstant(it.toInstant(), ZoneId.systemDefault()) }
                ),
                aktorId = aktorId
            )
        }
    }
}
