package no.nav.fo.veilarbdialog.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Component;

import lombok.val;
import no.nav.apiapp.security.PepClient;
import no.nav.brukerdialog.security.context.SubjectRule;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.dialogarena.config.security.ISSOProvider;
import no.nav.fo.veilarbdialog.client.KvpClient;
import no.nav.fo.veilarbdialog.db.DbTest;
import no.nav.fo.veilarbdialog.db.dao.DataVarehusDAO;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.DialogFeedDAO;
import no.nav.fo.veilarbdialog.db.dao.StatusDAO;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;
import no.nav.fo.veilarbdialog.rest.RestService;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.fo.veilarbdialog.service.DialogStatusService;

public class RestServiceTest extends DbTest {

    @Inject
    private RestService restService;

    @Inject
    private MockHttpServletRequest mockHttpServletRequest;

    @Rule
    public SubjectRule subjectRule = new SubjectRule();

    @Configuration
    static class ContextConfig {

        private static final String AKTORID = "123";
        private static final String FNR = "***REMOVED***";

        @Bean
        public AktorService aktorService() {
            AktorService aktorService = mock(AktorService.class);
            when(aktorService.getAktorId(FNR)).thenReturn(Optional.of(AKTORID));
            when(aktorService.getFnr(AKTORID)).thenReturn(Optional.of(FNR));
            return aktorService;
        }

        @Bean
        public PepClient pepClient() {
            return mock(PepClient.class);
        }

        @Bean
        public KvpClient kvpClient() {
            return mock(KvpClient.class);
        }

    }

    @BeforeClass
    public static void addSpringBeans() {
        initSpringContext(Arrays.asList(ContextConfig.class,
                AppService.class,
                DialogDAO.class,
                DialogStatusService.class,
                StatusDAO.class,
                DataVarehusDAO.class,
                DialogFeedDAO.class,
                Request.class,
                RestService.class,
                RestMapper.class,
                KontorsperreFilter.class));
    }

    @Component
    public static class Request extends MockHttpServletRequest {
    }

    @Before
    public void setup() {
        subjectRule.setSubject(new Subject("veileder", IdentType.InternBruker, SsoToken.oidcToken(ISSOProvider.getISSOToken())));
        mockHttpServletRequest.setParameter("fnr", "***REMOVED***");
    }

    @Test
    public void opprettOgHentDialoger() throws Exception {
        restService.nyHenvendelse(new NyHenvendelseDTO().setTekst("tekst"));
        val hentAktiviteterResponse = restService.hentDialoger();
        assertThat(hentAktiviteterResponse, hasSize(1));

        restService.markerSomLest(hentAktiviteterResponse.get(0).id);
    }

}