package no.nav.fo.veilarbdialog.graphql;

import no.nav.fo.veilarbdialog.SpringBootTestBase;
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
    void setupl() {
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

    @Test
    void veileder_skal_kun_hente_dialoger_for_bruker() {
        var result = graphqlRequest(veileder, allDialogFields);
        assertThat(result).isNotNull();
        assertThat(result.data).isNotNull();
        assertThat(result.errors).isNull();
    }

    @Test
    void bruker_skal_kun_hente_dialoger_for_seg_selv() {
        var result = graphqlRequest(bruker, allDialogFields);
        assertThat(result).isNotNull();
        assertThat(result.data).isNotNull();
        assertThat(result.errors).isNull();
    }

    @Test
    void ukjent_bruker_skal_ikke_kun_hente_dialoger_for_andre() {
        var ukjentbruker = MockNavService.createHappyBruker();
        var result = graphqlRequest(ukjentbruker, allDialogFields);
        assertThat(result.data.dialoger).isNull();
        assertThat(result.errors).isNotNull();
        assertThat(result.errors.get(0).message).isEqualTo("Ikke tilgang");
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
