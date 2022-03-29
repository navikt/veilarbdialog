package no.nav.fo.veilarbdialog.service;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonRepository;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.fo.veilarbdialog.db.dao.DataVarehusDAO;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.StatusDAO;
import no.nav.fo.veilarbdialog.db.dao.VarselDAO;
import no.nav.fo.veilarbdialog.domain.DatavarehusEvent;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.DialogStatus;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.EskaleringsvarselRepository;
import no.nav.fo.veilarbdialog.metrics.FunksjonelleMetrikker;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DialogStatusService {

    private final StatusDAO statusDAO;
    private final DialogDAO dialogDAO;
    private final DataVarehusDAO dataVarehusDAO;
    private final VarselDAO varselDAO;
    private final BrukernotifikasjonRepository brukernotifikasjonRepository;
    private final EskaleringsvarselRepository eskaleringsvarselRepository;
    private final FunksjonelleMetrikker funksjonelleMetrikker;
    private final BrukernotifikasjonService brukernotifikasjonService;

    public DialogData nyHenvendelse(DialogData dialogData, HenvendelseData henvendelseData) {
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
        dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.LEST_AV_VEILEDER);
        funksjonelleMetrikker.markerDialogSomLestAvVeileder(dialogData);
        return dialogDAO.hentDialog(dialogData.getId());
    }

    public DialogData markerSomLestAvBruker(DialogData dialogData) {
        if (dialogData.erLestAvBruker()) {
            return dialogData;
        }

        brukernotifikasjonRepository
                .hentBrukernotifikasjonBeskjedForDialogId(dialogData.getId())
                .ifPresent(brukernotifikasjon -> brukernotifikasjonService.bestillDone(brukernotifikasjon.id()));

        eskaleringsvarselRepository.hentGjeldende(AktorId.of(dialogData.getAktorId())).ifPresent(
                eskaleringsvarselEntity -> {
                    if (eskaleringsvarselEntity.tilhorendeDialogId() == dialogData.getId()) {
                        brukernotifikasjonService.bestillDone(eskaleringsvarselEntity.tilhorendeBrukernotifikasjonId());
                    }
                }
        );
//        if (harAktivtparagraf8Varsel(dialogData)) { // todo - antar vi ikke skal fjerne det her før vi finner ut av hva vi skal med aktive gamle forhåndsvarsler
//            int antall = varselDAO.hentAntallAktiveDialogerForVarsel(dialogData.getParagraf8VarselUUID());
//            if (antall == 1) {
//                varselDAO.revarslingSkalAvsluttes(dialogData.getParagraf8VarselUUID());
//            }
//        }
        statusDAO.markerSomLestAvBruker(dialogData.getId());

        dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.LEST_AV_BRUKER);
        funksjonelleMetrikker.markerDialogSomLestAvBruker(dialogData);
        return dialogDAO.hentDialog(dialogData.getId());
    }

    private boolean harAktivtparagraf8Varsel(DialogData dialogData) {
        return dialogData.isHarUlestParagraf8Henvendelse() && dialogData.getParagraf8VarselUUID() != null;
    }

    public DialogData oppdaterVenterPaNavSiden(DialogData dialogData, boolean ferdigBehandlet) {
        if (dialogData.erFerdigbehandlet() == ferdigBehandlet) {
            return dialogData;
        }

        if (ferdigBehandlet) {
            statusDAO.setVenterPaNavTilNull(dialogData.getId());
            dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.BESVART_AV_NAV);
        } else {
            statusDAO.setVenterPaNavTilNaa(dialogData.getId());
            dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.VENTER_PAA_NAV);
        }
        funksjonelleMetrikker.oppdaterFerdigbehandletTidspunkt(dialogData, ferdigBehandlet);
        return dialogDAO.hentDialog(dialogData.getId());
    }

    public DialogData oppdaterVenterPaSvarFraBrukerSiden(DialogData dialogData, DialogStatus dialogStatus) {
        if (dialogData.venterPaSvarFraBruker() == dialogStatus.venterPaSvar) {
            return dialogData;
        }

        if (dialogStatus.venterPaSvar) {
            statusDAO.setVenterPaSvarFraBrukerTilNaa(dialogStatus.getDialogId());
            dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.VENTER_PAA_BRUKER);
        } else {
            statusDAO.setVenterPaSvarFraBrukerTilNull(dialogStatus.getDialogId());
            dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.BESVART_AV_BRUKER);
        }
        funksjonelleMetrikker.oppdaterVenterSvar(dialogStatus);
        return dialogDAO.hentDialog(dialogStatus.getDialogId());
    }

    public void settDialogTilHistorisk(DialogData dialogData) {
        statusDAO.setHistorisk(dialogData.getId());
        if (!dialogData.erFerdigbehandlet()) {
            dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.BESVART_AV_NAV);
        }
        if (dialogData.venterPaSvarFraBruker()) {
            dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.BESVART_AV_BRUKER);
        }
        dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.SATT_TIL_HISTORISK);
        dialogDAO.hentDialog(dialogData.getId());
    }

    public void oppdaterDatavarehus(DialogData oprettet) {
        dataVarehusDAO.insertEvent(oprettet, DatavarehusEvent.DIALOG_OPPRETTET);
    }

    private void nyMeldingFraVeileder(DialogData dialogData, HenvendelseData henvendelseData) {
        dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.NY_HENVENDELSE_FRA_VEILEDER);

        Date eldsteUlesteForBruker = getEldsteUlesteForBruker(dialogData, henvendelseData);

        oppdaterVenterPaNavSiden(dialogData, true);

        statusDAO.setEldsteUlesteForBruker(dialogData.getId(), eldsteUlesteForBruker);
        funksjonelleMetrikker.nyHenvendelseVeileder(dialogData);
    }

    private Date getEldsteUlesteForBruker(DialogData dialogData, HenvendelseData henvendelseData) {
        return dialogData.erLestAvBruker() ? henvendelseData.getSendt() : dialogData.getEldsteUlesteTidspunktForBruker();
    }

    private void nyMeldingFraBruker(DialogData dialogData, HenvendelseData henvendelseData) {
        dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.NY_HENVENDELSE_FRA_BRUKER);

        Date nyesteUlestAvVeileder = hentTidspunktForNyesteUlestAvVeileder(dialogData, henvendelseData);
        Date venterPaNavSiden = dialogData.getVenterPaNavSiden();

        if (dialogData.erFerdigbehandlet()) {
            venterPaNavSiden = henvendelseData.getSendt();
            dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.VENTER_PAA_NAV);
        }
        if (dialogData.venterPaSvarFraBruker()) {
            dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.BESVART_AV_BRUKER);
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

    public void markerSomParagraf8(long dialogId) {
        statusDAO.markerSomParagraf8(dialogId);
    }
}
