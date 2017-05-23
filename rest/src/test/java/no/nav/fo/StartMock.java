package no.nav.fo;

import no.nav.fo.veilarbdialog.util.VarselMock;

import static no.nav.dialogarena.mock.MockServer.startMockServer;
import static no.nav.fo.StartJetty.CONTEXT_NAME;
import static no.nav.fo.StartJetty.PORT;

public class StartMock {

    public static void main(String[] args) throws Exception {
        VarselMock.init();
        startMockServer(CONTEXT_NAME, PORT);
    }

}
