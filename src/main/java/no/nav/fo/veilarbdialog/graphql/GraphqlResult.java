package no.nav.fo.veilarbdialog.graphql;

import lombok.Data;
import no.nav.domain.DialogDtoGraphql;
import no.nav.fo.veilarbdialog.domain.KladdDTO;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.EskaleringsvarselDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.GjeldendeEskaleringsvarselDto;

import java.util.List;

@Data
class Error {
    String message;
    List<Location> locations;
    List<String> path;
    Extension extensions;
}

@Data
class Location {
    String line;
    int column;
}

@Data
class Extension {
    String classification;
}

@Data
class Dialoger {
    List<DialogDtoGraphql> dialoger;
    GjeldendeEskaleringsvarselDto stansVarsel;
    List<KladdDTO> kladder;
    List<EskaleringsvarselDto> stansVarselHistorikk;
}

@Data
class GraphqlResult {
    Dialoger data;
    List<Error> errors;
}