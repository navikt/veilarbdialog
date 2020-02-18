package no.nav.fo.veilarbdialog.kafka;

import lombok.Builder;
import lombok.Data;
import no.nav.fo.veilarbdialog.domain.DialogData;

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
                .tidspunktEldsteVentende(dialoger.stream()
                        .filter(DialogData::venterPaSvar)
                        .map(DialogData::getVenterPaSvarFraBrukerSiden)
                        .min(naturalOrder())
                        .map(dato -> LocalDateTime.ofInstant(dato.toInstant(), ZoneId.systemDefault()))
                        .orElse(null)
                )
                .tidspunktEldsteUbehandlede(dialoger.stream()
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
