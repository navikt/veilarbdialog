package no.nav.fo.veilarbdialog.brukernotifikasjon;

import lombok.Builder;
import lombok.Getter;

import java.net.URL;

@Getter
@Builder
public class BeskjedInfo {
    private final String brukernotifikasjonId;
    private final String melding;
    private final String oppfolgingsperiode;
    private final String epostTitel;
    private final String epostBody;
    private final String smsTekst;
    private final URL link;
}
