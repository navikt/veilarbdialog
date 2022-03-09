package no.nav.fo.veilarbdialog.brukernotifikasjon.oppgave;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
class OppgaveInfo {
    private final long id;
    private final String brukernotifikasjonId;
    private final long aktivitetId;
    private final String melding;
    private final String oppfolgingsperiode;
    private final String aktorId;
    private final String epostTitel;
    private final String epostBody;
    private final String smsTekst;
}
