package no.nav.fo.veilarbdialog.mock_nav_modell;

import no.nav.poao_tilgang.core.domain.AdGrupper;
import no.nav.poao_tilgang.core.provider.NavEnhetTilgang;
import no.nav.poao_tilgang.poao_tilgang_test_core.AdGruppeProviderImpl;
import no.nav.poao_tilgang.poao_tilgang_test_core.NavAnsatt;
import no.nav.poao_tilgang.poao_tilgang_test_core.NavContext;
import no.nav.poao_tilgang.poao_tilgang_test_core.PrivatBruker;

import static java.util.Collections.emptyList;

public class MockNavService {
    public static final NavContext NAV_CONTEXT = new NavContext();
    private static final AdGrupper tilgjengligeAdGrupper = new AdGruppeProviderImpl(NAV_CONTEXT).hentTilgjengeligeAdGrupper();
    public static MockBruker createHappyBruker() {
        return createBruker(BrukerOptions.happyBruker());
    }

    public static MockBruker createBruker(BrukerOptions brukerOptions) {
        PrivatBruker ny = NAV_CONTEXT.getPrivatBrukere().ny();
        String aktorId = aktorIdFromFnr(aktorIdFromFnr(ny.getNorskIdent()));

        BrukerOptions.BrukerOptionsBuilder builder = brukerOptions.toBuilder();

        if(brukerOptions.getOppfolgingsEnhet() != null && !brukerOptions.getOppfolgingsEnhet().isBlank()) {
            ny.setOppfolgingsenhet(brukerOptions.getOppfolgingsEnhet());
        } else {
            builder.oppfolgingsEnhet(ny.getOppfolgingsenhet());
        }

        MockBruker mockBruker = new MockBruker(ny.getNorskIdent(), aktorId, builder.build());

        WireMockUtil.stubBruker(mockBruker);
        return mockBruker;
    }

    public static void updateBruker(MockBruker mockBruker, BrukerOptions brukerOptions) {
        mockBruker.setBrukerOptions(brukerOptions);
        if(brukerOptions.getOppfolgingsEnhet() != null && !brukerOptions.getOppfolgingsEnhet().isBlank()) {
            NAV_CONTEXT.getPrivatBrukere().get(mockBruker.getFnr()).setOppfolgingsenhet(brukerOptions.getOppfolgingsEnhet());
        }
        WireMockUtil.stubBruker(mockBruker);
    }

    public static MockVeileder createVeileder(MockBruker mockBruker) {
        PrivatBruker privatBruker = NAV_CONTEXT.getPrivatBrukere().get(mockBruker.getFnr());
        NavAnsatt navAnsatt = NAV_CONTEXT.getNavAnsatt().nyFor(privatBruker);
        return new MockVeileder(navAnsatt.getNavIdent());
    }

    public static MockVeileder createNKS() {
        NavAnsatt navAnsatt = NAV_CONTEXT.getNavAnsatt().nyNksAnsatt();
        return  new MockVeileder(navAnsatt.getNavIdent());
    }

    public static MockVeileder createVeileder() {
        PrivatBruker ny = NAV_CONTEXT.getPrivatBrukere().ny();
        NavAnsatt navAnsatt = NAV_CONTEXT.getNavAnsatt().nyFor(ny);


        return new MockVeileder(navAnsatt.getNavIdent());
    }

    public static MockVeileder createVeilederMedLesetilgang() {
        NavAnsatt navAnsatt = new NavAnsatt();
        NAV_CONTEXT.getNavAnsatt().add(navAnsatt);
        // etter at nasjonal-rollen forsvant fra poao-tilgang har det blitt lagt til at tilgang til enheten "NAV Viken"
        // vil tilsvare nasjonal tilgang. Antar det er en midlertidig løsning frem til noen lager noe mer presist.
        // https://github.com/navikt/poao-tilgang/blob/main/poao-tilgang-test-core/src/main/kotlin/no/nav/poao_tilgang/poao_tilgang_test_core/Providers.kt#L90
        navAnsatt.getEnheter().add(new NavEnhetTilgang("0000", "NAV Viken", emptyList()));
        return new MockVeileder(navAnsatt.getNavIdent());
    }

    private static String aktorIdFromFnr(String fnr) {
        return new StringBuilder(fnr).reverse().toString();
    }
}
