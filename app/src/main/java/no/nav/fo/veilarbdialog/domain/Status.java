package no.nav.fo.veilarbdialog.domain;

import lombok.Data;

import java.util.Date;

@Data
public class Status {

    public Status(long dialogId) {
        this.dialogId = dialogId;
    }

    public final long dialogId;
    public int historisk;
    public Date venterPaSvarFraBruker;
    public Date venterPaNavSiden;
    public Date eldsteUlesteForVeileder;
    public Date eldsteUlesteForBruker;
    private Date lestAvVeilederTid;
    private Date lestAvBrukerTid;

    public void setHistorisk(boolean historisk) {
        this.historisk = historisk ? 1 : 0;
    }

    public void settVenterPaSvarFraBruker(Date date) {
        if (venterPaSvarFraBruker == null) {
            venterPaSvarFraBruker = date;
        }
    }

    public void settVenterPaNavSiden(Date date) {
        if (venterPaNavSiden == null) {
            venterPaNavSiden = date;
        }
    }

    public void setUlesteMeldingerForVeileder(Date date) {
        if (eldsteUlesteForVeileder == null) {
            eldsteUlesteForVeileder = date;
        }
    }

    public void setUlesteMeldingerForBruker(Date date) {
        if (eldsteUlesteForBruker == null) {
            eldsteUlesteForBruker = date;
        }
    }

    public void resetVenterPaBrukerSiden() {
        venterPaSvarFraBruker = null;
    }
}
