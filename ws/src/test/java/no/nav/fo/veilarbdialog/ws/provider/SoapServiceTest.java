package no.nav.fo.veilarbdialog.ws.provider;

import lombok.val;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.DbTest;
import no.nav.fo.veilarbdialog.client.KvpClient;
import no.nav.fo.veilarbdialog.db.dao.*;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.fo.veilarbdialog.service.DialogStatusService;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.meldinger.HentDialogerForBrukerRequest;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.meldinger.OpprettDialogForAktivitetsplanRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class SoapServiceTest extends DbTest {

    private static final String FNR = "***REMOVED***";

    @Inject
    private SoapService soapService;

    @Configuration
    static class ContextConfig {

        private static final String AKTORID = "123";

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
                KontorsperreFilter.class,
                SoapService.class,
                SoapServiceMapper.class,
                VarselDAO.class));
    }

    @Test
    public void opprettOgHentDialoger() throws Exception {
        soapService.opprettDialogForAktivitetsplan(opprettDialogForAktivitetsplanRequest());
        val hentAktiviteterResponse = soapService.hentDialogerForBruker(hentDialogerForBrukerRequest());
        assertThat(hentAktiviteterResponse.getDialogListe(), hasSize(1));
    }

    private OpprettDialogForAktivitetsplanRequest opprettDialogForAktivitetsplanRequest() {
        val opprettDialogForAktivitetsplanRequest = new OpprettDialogForAktivitetsplanRequest();
        opprettDialogForAktivitetsplanRequest.setTittel("tittel");
        opprettDialogForAktivitetsplanRequest.setPersonIdent(FNR);
        return opprettDialogForAktivitetsplanRequest;
    }

    private HentDialogerForBrukerRequest hentDialogerForBrukerRequest() {
        val hentAktiviteterRequest = new HentDialogerForBrukerRequest();
        hentAktiviteterRequest.setPersonIdent(FNR);
        return hentAktiviteterRequest;
    }

}