package no.nav.fo.veilarbdialog.service;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.fo.veilarbdialog.minsidevarsler.MinsideVarselService;
import no.nav.fo.veilarbdialog.db.dao.DataVarehusDAO;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.StatusDAO;
import no.nav.fo.veilarbdialog.domain.DatavarehusEvent;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.DialogStatus;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.metrics.FunksjonelleMetrikker;
import no.nav.poao.dab.spring_auth.IAuthService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class DialogStatusService {

    private final StatusDAO statusDAO;
    private final DialogDAO dialogDAO;
    private final DataVarehusDAO dataVarehusDAO;
    private final FunksjonelleMetrikker funksjonelleMetrikker;
    private final MinsideVarselService minsideVarselService;
    private final IAuthService auth;

    private String getEndretAv() {
        return auth.erLoggetInn() ? auth.getLoggedInnUser().get() : "SYSTEM";
    }

    public DialogData oppdaterDialogTrådStatuserForNyMelding(DialogData dialogData, HenvendelseData henvendelseData) {
        if (henvendelseData.getSendt() == null) {
            throw new UnsupportedOperationException("sendt tidspunkt kan ikke være null");
        }
        if (henvendelseData.fraBruker()) {
            nyMeldingFraBruker(dialogData, henvendelseData);
        } else {
            nyMeldingFraVeileder(dialogData, henvendelseData);
        }
        return dialogDAO.hentDialog(dialogData.getId());
    }

    public DialogData markerSomLestAvVeileder(DialogData dialogData) {
        if (dialogData.erNyesteHenvendelseLestAvVeileder()) {
            return dialogData;
        }
        statusDAO.markerSomLestAvVeileder(dialogData.getId(), new Date());
        dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.LEST_AV_VEILEDER, getEndretAv());
        funksjonelleMetrikker.markerDialogSomLestAvVeileder(dialogData);
        return dialogDAO.hentDialog(dialogData.getId());
    }

    public DialogData markerSomLestAvBruker(DialogData dialogData) {
        if (dialogData.erLestAvBruker()) {
            return dialogData;
        }

        minsideVarselService.inaktiverVarselForDialogEllerForhåndsvarsel(dialogData.getId(), AktorId.of(dialogData.getAktorId()));

        statusDAO.markerSomLestAvBruker(dialogData.getId());

        dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.LEST_AV_BRUKER, getEndretAv());
        funksjonelleMetrikker.markerDialogSomLestAvBruker(dialogData);
        return dialogDAO.hentDialog(dialogData.getId());
    }

    public DialogData oppdaterVenterPaNavSiden(DialogData dialogData, boolean ferdigBehandlet) {
        if (dialogData.erFerdigbehandlet() == ferdigBehandlet) {
            return dialogData;
        }

        if (ferdigBehandlet) {
            statusDAO.setVenterPaNavTilNull(dialogData.getId());
            dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.BESVART_AV_NAV, getEndretAv());
        } else {
            statusDAO.setVenterPaNavTilNaa(dialogData.getId());
            dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.VENTER_PAA_NAV, getEndretAv());
        }
        funksjonelleMetrikker.oppdaterFerdigbehandletTidspunkt(dialogData, ferdigBehandlet);
        return dialogDAO.hentDialog(dialogData.getId());
    }

    public DialogData oppdaterVenterPaSvarFraBrukerSiden(DialogData dialogData, Boolean venterPaSvar) {
        if (dialogData.venterPaSvarFraBruker() == venterPaSvar) {
            return dialogData;
        }

        if (venterPaSvar) {
            statusDAO.setVenterPaSvarFraBrukerTilNaa(dialogData.getId());
            dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.VENTER_PAA_BRUKER, getEndretAv());
        } else {
            statusDAO.setVenterPaSvarFraBrukerTilNull(dialogData.getId());
            dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.BESVART_AV_BRUKER, getEndretAv());
        }
        funksjonelleMetrikker.oppdaterVenterSvar(venterPaSvar);
        return dialogDAO.hentDialog(dialogData.getId());
    }

    public void settDialogTilHistorisk(DialogData dialogData) {
        statusDAO.setHistorisk(dialogData.getId());
        if (!dialogData.erFerdigbehandlet()) {
            dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.BESVART_AV_NAV, getEndretAv());
        }
        if (dialogData.venterPaSvarFraBruker()) {
            dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.BESVART_AV_BRUKER, getEndretAv());
        }
        dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.SATT_TIL_HISTORISK, getEndretAv());
        dialogDAO.hentDialog(dialogData.getId());
    }

    public void oppdaterDatavarehus(DialogData oprettet) {
        dataVarehusDAO.insertEvent(oprettet, DatavarehusEvent.DIALOG_OPPRETTET, getEndretAv());
    }

    /**
     * <ul>
     *   <li>Lager event i event-tabell</li>
     *   <li>Setter VENTER_PA_NAV_SIDEN til null i tabell</li>
     *   <li>Setter ELDSTE_ULESTE_FOR_BRUKER i tabell</li>
     *   <li>Oppdaterer "Funksjonelle metrikker"</li>
     * </ul>
    * */
    private void nyMeldingFraVeileder(DialogData dialogData, HenvendelseData henvendelseData) {
        dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.NY_HENVENDELSE_FRA_VEILEDER, getEndretAv());

        Date eldsteUlesteForBruker = getEldsteUlesteForBruker(dialogData, henvendelseData);

        oppdaterVenterPaNavSiden(dialogData, true);

        statusDAO.setEldsteUlesteForBruker(dialogData.getId(), eldsteUlesteForBruker);
        funksjonelleMetrikker.nyHenvendelseVeileder(dialogData);
    }

    private Date getEldsteUlesteForBruker(DialogData dialogData, HenvendelseData henvendelseData) {
        return dialogData.erLestAvBruker() ? henvendelseData.getSendt() : dialogData.getEldsteUlesteTidspunktForBruker();
    }

    /**
     * <ul>
     *   <li>Lager event i event-tabell</li>
     *   <li>Oppdaterer VENTER_PA_SVAR_FRA_BRUKER, ELDSTE_ULESTE_FOR_VEILEDER og VENTER_PA_NAV_SIDEN i tabell</li>
     *   <li>Oppdaterer funksjonelle metrikker</li>
     * </ul>
     * */
    private void nyMeldingFraBruker(DialogData dialogData, HenvendelseData henvendelseData) {
        dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.NY_HENVENDELSE_FRA_BRUKER, getEndretAv());

        Date nyesteUlestAvVeileder = hentTidspunktForNyesteUlestAvVeileder(dialogData, henvendelseData);
        Date venterPaNavSiden = dialogData.getVenterPaNavSiden();

        if (dialogData.erFerdigbehandlet()) {
            venterPaNavSiden = henvendelseData.getSendt();
            dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.VENTER_PAA_NAV, getEndretAv());
        }
        if (dialogData.venterPaSvarFraBruker()) {
            dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.BESVART_AV_BRUKER, getEndretAv());
        }

        statusDAO.setNyMeldingFraBruker(
                dialogData.getId(),
                nyesteUlestAvVeileder,
                venterPaNavSiden
        );
        funksjonelleMetrikker.nyHenvendelseBruker(dialogData);
    }

    private Date hentTidspunktForNyesteUlestAvVeileder(DialogData dialogData, HenvendelseData henvendelseData) {
        return dialogData.erNyesteHenvendelseLestAvVeileder() ? henvendelseData.getSendt() : dialogData.getSisteUlestAvVeilederTidspunkt();
    }
}
