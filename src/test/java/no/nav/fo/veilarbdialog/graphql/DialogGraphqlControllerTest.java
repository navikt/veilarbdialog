package no.nav.fo.veilarbdialog.graphql;

import no.nav.fo.veilarbdialog.SpringBootTestBase;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

public class DialogGraphqlControllerTest extends SpringBootTestBase {

    private MockBruker bruker;
    private MockVeileder veileder;

    @BeforeEach
    void setupl() {
        bruker = MockNavService.createHappyBruker();
        veileder = MockNavService.createVeileder(bruker);
        Mockito.when(unleash.isEnabled("veilarbdialog.dialogvarsling")).thenReturn(true);
    }

    @Test
    void skal_opprette_graphql() {
        var query = """
            query($fnr: String!) {
                dialoger(fnr: $fnr) {\s
                    id,
                    henvendelser {
                        id
                    }
                }
            }    
        """.trim().replace("\n", "");

        var result = bruker.createRequest()
                .body("{ \"query\": \""+ query  +"\", \"variables\": { \"fnr\": \"" + bruker.getFnr() + "\" } }")
                .post("/veilarbdialog/graphql")
                .then()
                .statusCode(200)
                .extract()
                .as(GraphqlResult.class);

        assertThat(result).isNotNull();
        assertThat(result.data).isNotNull();
        assertThat(result.errors).isNull();
    }
}
