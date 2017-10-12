package no.nav.fo.veilarbdialog.ws.consumer;

import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.TestData;
import org.junit.Test;

import javax.inject.Inject;

import static com.github.npathai.hamcrestopt.OptionalMatchers.hasValue;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static org.junit.Assert.assertThat;

public class AktorServiceTest extends IntegrasjonsTest {

    private static final String IKKE_EKSISTERENDE_IDENT = "12345678";

    // TODO gir det mening å flytte denne testen til aktor-klient?

    @Inject
    private AktorService aktorService;

    @Test
    public void hentAktoerIdForIdent_ikkeEksisterendeIdent_ikkeFunnet() {
        assertThat(aktorService.getAktorId(IKKE_EKSISTERENDE_IDENT), isEmpty());
    }

    @Test
    public void hentAktoerIdForIdent_kjentTestIdent_kjentTestAktor() {
        assertThat(aktorService.getAktorId(TestData.KJENT_IDENT), hasValue(TestData.KJENT_AKTOR_ID));
    }

    @Test
    public void hentIdentForAktorId_kjentTestAktor_kjentTestIdent() {
        assertThat(aktorService.getFnr(TestData.KJENT_AKTOR_ID), hasValue(TestData.KJENT_IDENT));
    }

}
