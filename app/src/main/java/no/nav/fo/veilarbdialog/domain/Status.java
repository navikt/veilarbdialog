package no.nav.fo.veilarbdialog.domain;

import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode
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

    public void setHistorisk(boolean historisk) {
        this.historisk = historisk ? 1 : 0;
    }

    public void setVenterPaSvarFraBruker() {
        if (venterPaSvarFraBruker == null) {
            venterPaSvarFraBruker = new Date();
        }
    }

    public void setVenterPaNavSiden() {
        if (venterPaNavSiden == null) {
            venterPaNavSiden = new Date();
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
}
