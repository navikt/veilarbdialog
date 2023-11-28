package no.nav.fo.veilarbdialog.mock_nav_modell;

import no.nav.poao_tilgang.poao_tilgang_test_core.NavAnsatt;
import no.nav.poao_tilgang.poao_tilgang_test_core.NavContext;
import no.nav.poao_tilgang.poao_tilgang_test_core.PrivatBruker;

public class MockNavService {
    public static final NavContext NAV_CONTEXT = new NavContext();
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

    private static String aktorIdFromFnr(String fnr) {
        return new StringBuilder(fnr).reverse().toString();
    }
}
