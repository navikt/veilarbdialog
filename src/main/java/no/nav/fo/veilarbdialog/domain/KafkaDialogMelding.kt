package no.nav.fo.veilarbdialog.domain

import lombok.Builder
import lombok.Data
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.function.Function

data class KafkaDialogMelding(
    private val aktorId: String? = null,
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
        fun mapTilDialogData(dialoger: MutableList<DialogData?>, aktorId: String?): KafkaDialogMelding? {
            return KafkaDialogMelding // Venter på svar fra bruker.
                // Oversikten bryr seg bare om denne har en dato eller er null
                .tidspunktEldsteVentende(
                    dialoger.stream()
                        .filter { dialog: DialogData? -> !dialog!!.isHistorisk() }
                        .filter { obj: DialogData? -> obj!!.venterPaSvarFraBruker() }
                        .map<Date?> { obj: DialogData? -> obj!!.getVenterPaSvarFraBrukerSiden() }
                        .min(Comparator.naturalOrder<Date?>())
                        .map<LocalDateTime>(Function { dato: Date? ->
                            LocalDateTime.ofInstant(
                                dato!!.toInstant(),
                                ZoneId.systemDefault()
                            )
                        })
                        .orElse(null)
                ) // Venter på svar fra nav.
                // Oversikten bryr seg bare om denne har en dato eller er null
                .tidspunktEldsteUbehandlede(
                    dialoger.stream()
                        .filter { dialog: DialogData? -> !dialog!!.isHistorisk() }
                        .filter { obj: DialogData? -> obj!!.erUbehandlet() }
                        .map<Date?> { obj: DialogData? -> obj!!.getVenterPaNavSiden() }
                        .min(Comparator.naturalOrder<Date?>())
                        .map<LocalDateTime>(Function { dato: Date? ->
                            LocalDateTime.ofInstant(
                                dato!!.toInstant(),
                                ZoneId.systemDefault()
                            )
                        })
                        .orElse(null)
                )
                .aktorId(aktorId)
                .build()
        }
    }
}
