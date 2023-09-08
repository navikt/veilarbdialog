package no.nav.fo.veilarbdialog.mock_nav_modell;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import no.nav.common.auth.context.UserRole;

import java.util.LinkedList;
import java.util.List;

@ToString
public class MockVeileder extends RestassuredUser {
    @Setter
    @Getter
    private boolean nasjonalTilgang = false;

    MockVeileder(String ident) {
        super(ident, UserRole.INTERN);
    }

    public String getNavIdent() {
        return super.ident;
    }

}
