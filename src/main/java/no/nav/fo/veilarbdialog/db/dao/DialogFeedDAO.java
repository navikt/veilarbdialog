package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.fo.veilarbdialog.domain.DialogAktor;
import no.nav.fo.veilarbdialog.domain.DialogData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import static java.util.Comparator.naturalOrder;
import static java.util.Optional.ofNullable;

@Component
@RequiredArgsConstructor
@Slf4j
public class DialogFeedDAO {

    private final JdbcTemplate jdbc;
    private final DateProvider dateProvider;

    private static Date hentDato(ResultSet rs, String kolonneNavn) throws SQLException {
        return ofNullable(rs.getTimestamp(kolonneNavn))
                .map(Timestamp::getTime)
                .map(Date::new)
                .orElse(null);
    }

    public List<DialogAktor> hentAktorerMedEndringerFOM(Date date, int pageSize) {
        return jdbc.query("select * from " +
                "(select * from DIALOG_AKTOR where opprettet_tidspunkt >= ? order by opprettet_tidspunkt) " +
                "where rownum <= ?",
        new Object[]{date, pageSize},
                (rs, rowNum) -> DialogAktor.builder()
                        .aktorId(rs.getString("aktor_id"))
                        .sisteEndring(hentDato(rs, "siste_endring"))
                        .tidspunktEldsteVentende(hentDato(rs, "tidspunkt_eldste_ventende"))
                        .tidspunktEldsteUbehandlede(hentDato(rs, "tidspunkt_eldste_ubehandlede"))
                        .opprettetTidspunkt(hentDato(rs, "opprettet_tidspunkt"))
                        .build());
    }

    public void updateDialogAktorFor(String aktorId, List<DialogData> dialoger) {
        if (dialoger.isEmpty()) {
            log.info("Finner ingen dialoger for aktør [{}]. Oppretter ikke innslag i DIALOG_AKTOR", aktorId);
            return;
        }
        val dialogAktor = mapTilDialogAktor(dialoger);
        jdbc.update("insert into DIALOG_AKTOR (aktor_id, siste_endring, tidspunkt_eldste_ventende, tidspunkt_eldste_ubehandlede, opprettet_tidspunkt) " +
                "values (?, ?, ?, ?, ?)",
                aktorId,
                dialogAktor.getSisteEndring(),
                dialogAktor.getTidspunktEldsteVentende(),
                dialogAktor.getTidspunktEldsteUbehandlede(),
                dateProvider.getNow());
    }

    private static DialogAktor mapTilDialogAktor(List<DialogData> dialoger) {
        return DialogAktor.builder()
                .sisteEndring(dialoger.stream()
                        .map(DialogData::getOppdatert)
                        .max(naturalOrder())
                        .orElse(null)
                )
                .tidspunktEldsteVentende(dialoger.stream()
                        .filter(DialogData::venterPaSvar)
                        .map(DialogData::getVenterPaSvarFraBrukerSiden)
                        .min(naturalOrder())
                        .orElse(null)
                )
                .tidspunktEldsteUbehandlede(dialoger.stream()
                        .filter(DialogData::erUbehandlet)
                        .map(DialogData::getVenterPaNavSiden)
                        .min(naturalOrder())
                        .orElse(null)
                ).build();
    }

}
