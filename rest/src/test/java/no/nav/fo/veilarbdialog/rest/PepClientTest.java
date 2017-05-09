package no.nav.fo.veilarbdialog.rest;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.fo.IntegrasjonsTest;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

import static no.nav.fo.TestData.*;

public class PepClientTest extends IntegrasjonsTest {

    @Inject
    private PepClient pepClient;

    @Before
    public void setup() {
        setVeilederSubject(KJENT_VEILEDER_IDENT);
    }

    @Test
    public void sjekkTilgangTilFnr_veilederHarTilgang() {
        pepClient.sjekkTilgangTilFnr(KJENT_IDENT_FOR_KJENT_VEILEDER);
    }

    @Test(expected = IngenTilgang.class)
    public void sjekkTilgangTilFnr_veilederHarIkkeTilgang() {
        pepClient.sjekkTilgangTilFnr(KJENT_IDENT);
    }


}