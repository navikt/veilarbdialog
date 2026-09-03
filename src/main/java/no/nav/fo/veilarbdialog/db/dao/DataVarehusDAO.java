package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.db.jdbc.EventRepository;
import no.nav.fo.veilarbdialog.domain.AktivitetId;
import no.nav.fo.veilarbdialog.domain.DatavarehusEvent;
import no.nav.fo.veilarbdialog.domain.DialogData;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

import no.nav.fo.veilarbdialog.db.jdbc.JdbcConverters;

@Component
@RequiredArgsConstructor
public class DataVarehusDAO {

    private final EventRepository eventRepository;

    public void insertEvent(DialogData dialogData, DatavarehusEvent datavarehusEvent, String endretAv) {
        long nextId = eventRepository.nextEventId();
        eventRepository.insert(
                nextId,
                dialogData.getId(),
                datavarehusEvent.toString(),
                dialogData.getAktorId(),
                Optional.ofNullable(dialogData.getAktivitetId()).map(AktivitetId::getId).orElse(null),
                endretAv);
    }

    @Transactional(readOnly = true)
    public Date hentSisteEndringSomIkkeErDine(String aktorId, String bruker) {
        return eventRepository.findSisteEndringSomIkkeErDine(aktorId, bruker)
                .map(JdbcConverters::toDate)
                .orElse(null);
    }
}
