package no.nav.fo.veilarbdialog.clients.dialogvarsler;

import no.nav.common.types.identer.Fnr;

public interface DialogVarslerClient {
    public void varsleLyttere(Fnr fnr, EventType eventType);

    enum EventType {
        NY_DIALOGMELDING_FRA_BRUKER_TIL_NAV,
        NY_DIALOGMELDING_FRA_NAV_TIL_BRUKER,
    }
}
