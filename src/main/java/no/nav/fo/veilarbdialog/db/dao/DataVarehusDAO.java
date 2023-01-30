package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Id;
import no.nav.fo.veilarbdialog.domain.AktivitetId;
import no.nav.fo.veilarbdialog.domain.DatavarehusEvent;
import no.nav.fo.veilarbdialog.domain.DialogData;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataVarehusDAO {

    private final JdbcTemplate jdbc;

    public void insertEvent(DialogData dialogData, DatavarehusEvent datavarehusEvent, String endretAv) {
        long nextId = Optional
                .ofNullable(jdbc.queryForObject("select EVENT_ID_SEQ.NEXTVAL from dual", Long.class))
                .orElseThrow(IllegalStateException::new);
        jdbc.update("insert into EVENT (EVENT_ID, DIALOGID, EVENT, TIDSPUNKT, AKTOR_ID, AKTIVITET_ID, LAGT_INN_AV) values (?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?)",
                nextId,
                dialogData.getId(),
                datavarehusEvent.toString(),
                dialogData.getAktorId(),
                Optional.ofNullable(dialogData.getAktivitetId()).map(AktivitetId::getId).orElse(null),
                endretAv);
    }

    @Transactional(readOnly = true)
    public Date hentSisteEndringSomIkkeErDine(String aktorId, String bruker) {
        try {
            Timestamp timestamp = Optional
                    .ofNullable(jdbc.queryForObject(
                            "select TIDSPUNKT from EVENT where AKTOR_ID = ? and LAGT_INN_AV != ? ORDER BY EVENT_ID DESC FETCH FIRST 1 ROWS ONLY",
                            Timestamp.class,
                            aktorId,
                            bruker))
                    .orElseThrow();
            return Date.from(timestamp.toInstant());
        } catch (EmptyResultDataAccessException | NoSuchElementException e) {
            return null;
        }
    }

}
