package no.nav.fo.veilarbdialog.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static java.util.Comparator.naturalOrder;

@Builder
@Data
public class KafkaDialogMelding {
    private String aktorId;
    private LocalDateTime tidspunktEldsteVentende;
    private LocalDateTime tidspunktEldsteUbehandlede;


    public static KafkaDialogMelding mapTilDialogData(List<DialogData> dialoger, String aktorId) {
        return KafkaDialogMelding.builder()
                // Venter på svar fra bruker.
                // Oversikten bryr seg bare om denne har en dato eller er null
                .tidspunktEldsteVentende(dialoger.stream()
                        .filter(dialog -> !dialog.isHistorisk())
                        .filter(DialogData::venterPaSvarFraBruker)
                        .map(DialogData::getVenterPaSvarFraBrukerSiden)
                        .min(naturalOrder())
                        .map(dato -> LocalDateTime.ofInstant(dato.toInstant(), ZoneId.systemDefault()))
                        .orElse(null)
                )
                // Venter på svar fra nav.
                // Oversikten bryr seg bare om denne har en dato eller er null
                .tidspunktEldsteUbehandlede(dialoger.stream()
                        .filter(dialog -> !dialog.isHistorisk())
                        .filter(DialogData::erUbehandlet)
                        .map(DialogData::getVenterPaNavSiden)
                        .min(naturalOrder())
                        .map(dato -> LocalDateTime.ofInstant(dato.toInstant(), ZoneId.systemDefault()))
                        .orElse(null)
                )
                .aktorId(aktorId)
                .build();
    }
}
