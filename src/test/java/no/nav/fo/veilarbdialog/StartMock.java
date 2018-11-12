package no.nav.fo.veilarbdialog;

import static no.nav.dialogarena.mock.MockServer.startMockServer;
import static no.nav.fo.veilarbdialog.StartJetty.PORT;
import static no.nav.fo.veilarbdialog.StartJetty.APPLICATION_NAME;

public class StartMock {

    public static void main(String[] args) throws Exception {
        startMockServer(APPLICATION_NAME, PORT);
    }

}
