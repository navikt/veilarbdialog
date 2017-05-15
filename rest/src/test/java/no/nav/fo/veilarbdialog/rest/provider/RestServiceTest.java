package no.nav.fo.veilarbdialog.rest.provider;

import lombok.val;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.rest.RestService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.inject.Inject;

import static no.nav.fo.TestData.KJENT_IDENT_FOR_KJENT_VEILEDER;
import static no.nav.fo.TestData.KJENT_VEILEDER_IDENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;


public class RestServiceTest extends IntegrasjonsTest {

    @Inject
    private RestService restService;

    @Inject
    private MockHttpServletRequest mockHttpServletRequest;

    @Before
    public void setup() {
        setVeilederSubject(KJENT_VEILEDER_IDENT);
        mockHttpServletRequest.setParameter("fnr", KJENT_IDENT_FOR_KJENT_VEILEDER);
    }

    @Test
    public void opprettOgHentDialoger() throws Exception {
        restService.nyHenvendelse(new NyHenvendelseDTO().setTekst("tekst"));
        val hentAktiviteterResponse = restService.hentDialoger();
        assertThat(hentAktiviteterResponse, hasSize(1));

        restService.markerSomLest(hentAktiviteterResponse.get(0).id);
    }

}