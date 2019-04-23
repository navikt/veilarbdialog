package no.nav.fo.veilarbdialog.rest;

import lombok.val;
import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import no.nav.brukerdialog.security.context.SubjectRule;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.IntegationTest;
import no.nav.fo.veilarbdialog.client.KvpClient;
import no.nav.fo.veilarbdialog.db.dao.*;
import no.nav.fo.veilarbdialog.domain.Egenskap;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.fo.veilarbdialog.service.AutorisasjonService;
import no.nav.fo.veilarbdialog.service.DialogStatusService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DialogRessursTest extends IntegationTest {

    private static final String AKTORID = "123";
    private static final String FNR = "4321";

    @Inject
    private DialogRessurs dialogRessurs;

    @Inject
    private MockHttpServletRequest mockHttpServletRequest;

    @Rule
    public SubjectRule subjectRule = new SubjectRule();

    static class ContextConfig {

        @Bean
        public AktorService aktorService() {
            AktorService aktorService = mock(AktorService.class);
            when(aktorService.getAktorId(FNR)).thenReturn(Optional.of(AKTORID));
            when(aktorService.getFnr(AKTORID)).thenReturn(Optional.of(FNR));
            return aktorService;
        }

        @Bean
        public VeilarbAbacPepClient pepClient() {
            return mock(VeilarbAbacPepClient.class);
        }

        @Bean
        public KvpClient kvpClient() {
            return mock(KvpClient.class);
        }

    }

    @BeforeClass
    public static void addSpringBeans() {
        initSpringContext(asList(ContextConfig.class,
                AppService.class,
                DialogDAO.class,
                DialogStatusService.class,
                StatusDAO.class,
                DataVarehusDAO.class,
                DialogFeedDAO.class,
                Request.class,
                DialogRessurs.class,
                AutorisasjonService.class,
                RestMapper.class,
                KontorsperreFilter.class,
                VarselDAO.class));
    }

    @Component
    public static class Request extends MockHttpServletRequest {
    }

    @Before
    public void setup() {
        subjectRule.setSubject(new Subject("veileder", IdentType.InternBruker, mock(SsoToken.class)));
        mockHttpServletRequest.setParameter("fnr", FNR);
    }

    @Test
    public void opprettOgHentDialoger() throws Exception {
        dialogRessurs.nyHenvendelse(new NyHenvendelseDTO().setTekst("tekst"));
        val hentAktiviteterResponse = dialogRessurs.hentDialoger();
        assertThat(hentAktiviteterResponse, hasSize(1));

        dialogRessurs.markerSomLest(hentAktiviteterResponse.get(0).id);
    }

    @Test
    public void forhandsorienteringPaEksisterendeDialogPaAktivitetSkalFaEgenskapenParagraf8() {
        final String aktivitetId = "123";

        dialogRessurs.nyHenvendelse(
                new NyHenvendelseDTO()
                        .setTekst("forhandsorienteringPaEksisterendeDialogPaAktivitetSkalFaEgenskapenParagraf8")
                        .setAktivitetId(aktivitetId)
        );

        val opprettetDialog = dialogRessurs.hentDialoger();
        assertThat(opprettetDialog.get(0).getEgenskaper().isEmpty(), is(true));
        assertThat(opprettetDialog.size(), is(1));

        dialogRessurs.forhandsorienteringPaAktivitet(
                new NyHenvendelseDTO()
                        .setTekst("paragraf8")
                        .setAktivitetId(aktivitetId)
        );

        val dialogMedParagraf8 = dialogRessurs.hentDialoger();
        assertThat(dialogMedParagraf8.get(0).getEgenskaper().contains(Egenskap.PARAGRAF8), is(true));
        assertThat(dialogMedParagraf8.size(), is(1));
    }

    @Test
    public void skalHaParagraf8Egenskap() {
        dialogRessurs.forhandsorienteringPaAktivitet(
                new NyHenvendelseDTO()
                        .setTekst("skalHaParagraf8Egenskap")
                        .setAktivitetId("123")
        );

        val hentedeDialoger = dialogRessurs.hentDialoger();
        assertThat(hentedeDialoger, hasSize(1));
        assertThat(hentedeDialoger.get(0).getEgenskaper().contains(Egenskap.PARAGRAF8), is(true));
    }
}
