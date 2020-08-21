package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.fo.veilarbdialog.domain.DatavarehusEvent;
import no.nav.fo.veilarbdialog.domain.DialogData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataVarehusDAO {

    static final String EVENT_ID = "event_id";
    static final String DIALOGID = "dialogid";
    static final String EVENT = "event";
    static final String TIDSPUNKT = "tidspunkt";
    static final String AKTOR_ID = "aktor_id";
    static final String AKTIVITET_ID = "aktivitet_id";
    static final String EVENT_TABELL = "EVENT";

    private final JdbcTemplate jdbc;

    public void insertEvent(DialogData dialogData, DatavarehusEvent datavarehusEvent) {
        long nextId = Optional
                .ofNullable(jdbc.queryForObject("select EVENT_ID_SEQ.NEXTVAL from dual", Long.class))
                .orElseThrow(IllegalStateException::new);
        jdbc.update("insert into EVENT (event_id, dialogid, event, tidspunkt, aktor_id, aktivitet_id, lagt_inn_av) values (?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?)",
                nextId,
                dialogData.getId(),
                datavarehusEvent.toString(),
                dialogData.getAktorId(),
                dialogData.getAktivitetId(),
                getLagtInnAv());
    }

    @Transactional(readOnly = true)
    public Date hentSisteEndringSomIkkeErDine(String aktorId, String bruker) {
        return jdbc.queryForObject("select TIDSPUNKT from EVENT where AKTOR_ID = ? and LAGT_INN_AV != ? ORDER BY EVENT_ID DESC FETCH FIRST 1 ROWS ONLY",
                new Object[]{
                        aktorId,
                        bruker
                },
                Date.class);
    }

    private static String getLagtInnAv() {
        return SubjectHandler.getIdent().orElse("SYSTEM");
    }

}
