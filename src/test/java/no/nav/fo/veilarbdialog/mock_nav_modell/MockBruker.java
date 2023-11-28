package no.nav.fo.veilarbdialog.mock_nav_modell;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import no.nav.common.auth.context.UserRole;
import no.nav.common.types.identer.AktorId;

import java.util.UUID;

@Getter
@ToString
public class MockBruker extends RestassuredUser {
    private final String aktorId;
    private final UUID oppfolgingsperiode = UUID.randomUUID();
    @Setter(AccessLevel.PACKAGE)
    private BrukerOptions brukerOptions;

    MockBruker(String fnr, String aktorId, BrukerOptions brukerOptions) {
        super(fnr, UserRole.EKSTERN);
        this.aktorId = aktorId;
        this.brukerOptions = brukerOptions;
    }

    public String getFnr() {
        return super.ident;
    }

    public AktorId getAktorIdAsAktorId() {
        return AktorId.of(aktorId);
    }

}
