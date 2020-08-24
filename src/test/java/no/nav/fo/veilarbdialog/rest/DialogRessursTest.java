package no.nav.fo.veilarbdialog.rest;

import lombok.val;
import no.nav.apiapp.security.PepClient;
import no.nav.brukerdialog.security.context.SubjectRule;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.IntegationTest;
import no.nav.fo.veilarbdialog.service.KvpService;
import no.nav.fo.veilarbdialog.db.dao.*;
import no.nav.fo.veilarbdialog.domain.Egenskap;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.db.dao.KafkaDAO;
import no.nav.fo.veilarbdialog.service.*;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.clients.producer.Producer;
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

    @Autowired
    private DialogRessurs dialogRessurs;

    @Autowired
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
        public PepClient pepClient() {
            return mock(PepClient.class);
        }

        @Bean
        public KvpService kvpClient() {
            return mock(KvpService.class);
        }

        @Bean
        public KafkaDialogService kafkaDialogService() {
            System.setProperty("APP_ENVIRONMENT_NAME", "TEST-Q0");
            return new KafkaDialogService(mock(Producer.class), mock(KafkaDAO.class), mock(DialogDAO.class));
        }

        @Bean
        public UnleashService unleashService() {
            return mock(UnleashService.class);
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
                KladdDAO.class,
                DialogFeedDAO.class,
                Request.class,
                KladdService.class,
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
