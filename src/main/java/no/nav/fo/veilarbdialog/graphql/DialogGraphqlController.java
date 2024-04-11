package no.nav.fo.veilarbdialog.graphql;

import no.nav.fo.veilarbdialog.domain.DialogDTO;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.Collections;
import java.util.List;

@Controller
public class DialogGraphqlController {

    @QueryMapping
    public List<DialogDTO> hentDialoger() {
        return Collections.emptyList();
    }

}
