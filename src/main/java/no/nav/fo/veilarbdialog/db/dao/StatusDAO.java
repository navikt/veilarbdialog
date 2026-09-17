package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.db.jdbc.DialogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static no.nav.fo.veilarbdialog.db.jdbc.JdbcConverters.toLocalDateTime;

@Component
@Transactional
@RequiredArgsConstructor
@java.lang.SuppressWarnings("squid:S1192")
public class StatusDAO {

    private final DialogRepository dialogRepository;

    public void markerSomLestAvVeileder(long dialogId, Date lestTidspunkt) {
        dialogRepository.markerSomLestAvVeileder(dialogId, null, toLocalDateTime(lestTidspunkt));
    }

    public void markerSomLestAvBruker(long dialogId) {
        dialogRepository.markerSomLestAvBruker(dialogId);
    }

    public void setVenterPaNavTilNaa(long dialogId) {
        dialogRepository.setVenterPaNavTilNaa(dialogId);
    }

    public void setVenterPaSvarFraBrukerTilNaa(long dialogId) {
        dialogRepository.setVenterPaSvarFraBrukerTilNaa(dialogId);
    }

    public void setVenterPaNavTilNull(long dialogId) {
        dialogRepository.setVenterPaNavTilNull(dialogId);
    }

    public void setVenterPaSvarFraBrukerTilNull(long dialogId) {
        dialogRepository.setVenterPaSvarFraBrukerTilNull(dialogId);
    }

    public void setEldsteUlesteForBruker(long dialogId, Date date) {
        dialogRepository.setEldsteUlesteForBruker(dialogId, toLocalDateTime(date));
    }

    public void setNyMeldingFraBruker(long dialogId, Date eldsteUlesteForVeileder, Date venterPaNavSiden) {
        dialogRepository.setNyMeldingFraBruker(
                dialogId,
                toLocalDateTime(eldsteUlesteForVeileder),
                toLocalDateTime(venterPaNavSiden));
    }

    public void setHistorisk(long dialogId) {
        dialogRepository.setHistorisk(dialogId);
    }

    public void markerSomParagraf8(long dialogId) {
        dialogRepository.markerSomParagraf8(dialogId);
    }
}
