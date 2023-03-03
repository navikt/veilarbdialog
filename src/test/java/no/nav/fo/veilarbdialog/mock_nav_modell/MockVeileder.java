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
    private final List<MockBruker> brukerList = new LinkedList<>();

    MockVeileder(String ident) {
        super(ident, UserRole.INTERN);
    }

    public String getNavIdent() {
        return super.ident;
    }

    public void addBruker(MockBruker bruker) {
        brukerList.add(bruker);
    }

    public boolean harTilgangTilBruker(MockBruker bruker) {
        return nasjonalTilgang || brukerList.stream().anyMatch(it -> it.equals(bruker));
    }

    public boolean harTilgangTilEnhet(String enhet) {
        return brukerList.stream().anyMatch(it -> it.getBrukerOptions().getKontorsperreEnhet().equals(enhet));
    }
}
