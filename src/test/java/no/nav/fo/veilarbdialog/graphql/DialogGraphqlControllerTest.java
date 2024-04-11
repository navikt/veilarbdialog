package no.nav.fo.veilarbdialog.graphql;

import no.nav.fo.veilarbdialog.SpringBootTestBase;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
            query($fnrParam: String!) {
                dialoger(fnr: $fnrParam) {\s
                    id,
                    henvendelser {
                        id
                    }
                }
            }    
        """.trim().replace("\n", "");

        List<DialogDTO> dialoger = bruker.createRequest()
                .body("{ \"query\": \""+ query  +"\", \"variables\": { \"fnr\": \"" + bruker.getFnr() + "\" } }")
                .post("/veilarbdialog/graphql")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", DialogDTO.class);

    }
}
