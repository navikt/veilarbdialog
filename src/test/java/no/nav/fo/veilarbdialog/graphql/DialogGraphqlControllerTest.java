package no.nav.fo.veilarbdialog.graphql;

import no.nav.fo.veilarbdialog.SpringBootTestBase;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import no.nav.fo.veilarbdialog.mock_nav_modell.RestassuredUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.assertj.core.api.Assertions.assertThat;

public class DialogGraphqlControllerTest extends SpringBootTestBase {

    private MockBruker bruker;
    private MockVeileder veileder;

    @BeforeEach
    void setupl() { // Må bruke et annet navn en "setup" fordi det brukes i super-klassen
        bruker = MockNavService.createHappyBruker();
        veileder = MockNavService.createVeileder(bruker);
        Mockito.when(unleash.isEnabled("veilarbdialog.dialogvarsling")).thenReturn(true);
    }

    private GraphqlResult graphqlRequest(RestassuredUser user, String query) {
        return user.createRequest()
            .body("{ \"query\": \""+ query  +"\", \"variables\": { \"fnr\": \"" + bruker.getFnr() + "\" } }")
            .post("/veilarbdialog/graphql")
            .then()
            .statusCode(200)
            .extract()
            .as(GraphqlResult.class);
    }

    private void nyTraad(RestassuredUser user) {
        user.createRequest()
                .body(new NyHenvendelseDTO().setTekst("tekst"))
                .queryParam("aktorId", bruker.getAktorId())
                .post("/veilarbdialog/api/dialog")
                .then()
                .statusCode(200);
    }

    @Test
    void veileder_skal_kun_hente_dialoger_for_bruker() {
        nyTraad(veileder);
        var result = graphqlRequest(veileder, allDialogFields);
        assertThat(result.data.dialoger).hasSize(1);
        assertThat(result.errors).isNull();
    }

    @Test
    void bruker_skal_kun_hente_dialoger_for_seg_selv() {
        nyTraad(bruker);
        var result = graphqlRequest(bruker, allDialogFields);
        assertThat(result.data.dialoger).hasSize(1);
        assertThat(result.errors).isNull();
    }

    @Test
    void bruker_skal_bare_kunne_hente_dialoger_for_seg_selv() {
        nyTraad(bruker);
        var brukerUtenDialoger = MockNavService.createHappyBruker();
        var result = graphqlRequest(brukerUtenDialoger, allDialogFields);
        assertThat(result.data.dialoger).hasSize(0);
        assertThat(result.errors).isNull();
    }

    @Test
    void veileder_uten_tilgang_til_bruker_skal_ikke_kunne_hente_dialoger() {
        var veilederUtenTilgang = MockNavService.createVeileder();
        var result = graphqlRequest(veilederUtenTilgang, allDialogFields);
        assertThat(result).isNotNull();
        assertThat(result.errors).hasSize(1);
        assertThat(result.errors.get(0).message).isEqualTo("Ikke tilgang");
    }

    static String allDialogFields = """
            query($fnr: String!) {
                dialoger(fnr: $fnr) {
                    aktivitetId,
                    oppfolgingsperiode,
                    opprettetDato,
                    egenskaper,
                    erLestAvBruker,
                    ferdigBehandlet,
                    historisk,
                    lest,
                    lestAvBrukerTidspunkt,
                    sisteTekst,
                    sisteDato,
                    venterPaSvar,
                    henvendelser {
                        id,
                        lest,
                        avsender,
                        avsenderId,
                        dialogId,
                        sendt,
                        viktig,
                        tekst
                    }
                }
            }   
        """.trim().replace("\n", "");
}
