package no.nav.fo.veilarbdialog.graphql;

import lombok.Data;
import no.nav.fo.veilarbdialog.domain.DialogDTO;

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
    List<DialogDTO> dialoger;
}

@Data
class GraphqlResult {
    Dialoger data;
    List<Error> errors;
}