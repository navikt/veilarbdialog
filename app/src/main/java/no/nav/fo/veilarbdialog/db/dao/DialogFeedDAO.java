package no.nav.fo.veilarbdialog.db.dao;

import lombok.val;
import no.nav.fo.veilarbdialog.domain.DialogAktor;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.sbl.jdbc.Database;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static java.util.Comparator.naturalOrder;
import static no.nav.sbl.jdbc.Database.hentDato;

@Component
public class DialogFeedDAO {
    
    private final Database database;
    private final DateProvider dateProvider;

    @Inject
    public DialogFeedDAO(Database database, DateProvider dateProvider) {
        this.database = database;
        this.dateProvider = dateProvider;
    }


    public List<DialogAktor> hentAktorerMedEndringerFOM(Date date, int pageSize) {
        return database.query("SELECT * FROM " +
                        "(SELECT * FROM DIALOG_AKTOR WHERE siste_endring >= ? ORDER BY opprettet_tidspunkt) " +
                        "WHERE rownum <= ?",
                this::mapTilDialogAktor,
                date,
                pageSize
        );
    }

    public void updateDialogAktorFor(String aktorId, List<DialogData> dialoger){
        val dialogAktor = mapTilDialogAktor(dialoger);
        database.update("INSERT INTO DIALOG_AKTOR (" +
                        "aktor_id, " +
                        "siste_endring, " +
                        "tidspunkt_eldste_ventende, " +
                        "tidspunkt_eldste_ubehandlede, " +
                        "opprettet_tidspunkt) " +
                        "VALUES (?,?,?,?, "+ dateProvider.getNow() +")",
                aktorId,
                dialogAktor.getSisteEndring(),
                dialogAktor.getTidspunktEldsteVentende(),
                dialogAktor.getTidspunktEldsteUbehandlede()
        );

    }

    private DialogAktor mapTilDialogAktor(ResultSet rs) throws SQLException{
        return DialogAktor.builder()
                .aktorId(rs.getString("aktor_id"))
                .sisteEndring(hentDato(rs, "siste_endring"))
                .tidspunktEldsteVentende(hentDato(rs, "tidspunkt_eldste_ventende"))
                .tidspunktEldsteUbehandlede(hentDato(rs, "tidspunkt_eldste_ubehandlede"))
                .opprettetTidspunkt(hentDato(rs, "opprettet_tidspunkt"))
                .build();
    }

    private static DialogAktor mapTilDialogAktor(List<DialogData> dialoger) {
        return DialogAktor.builder()
                .sisteEndring(dialoger.stream()
                        .map(DialogData::getSisteEndring)
                        .max(naturalOrder())
                        .orElse(null)
                )
                .tidspunktEldsteVentende(dialoger.stream()
                        .filter(DialogData::venterPaSvar)
                        .map(DialogData::getVenterPaSvarTidspunkt)
                        .min(naturalOrder())
                        .orElse(null)
                )
                .tidspunktEldsteUbehandlede(dialoger.stream()
                        .filter(DialogData::erUbehandlet)
                        .map(DialogData::getUbehandletTidspunkt)
                        .min(naturalOrder())
                        .orElse(null)
                ).build();
    }
}
