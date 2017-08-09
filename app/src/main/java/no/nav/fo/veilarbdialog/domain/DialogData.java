package no.nav.fo.veilarbdialog.domain;

import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.Wither;

import java.util.Date;
import java.util.List;

@Value
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@Accessors(chain = true)
@Wither
public class DialogData {

    public final long id;
    public final String aktorId;
    public final String overskrift;
    public final String aktivitetId;

    public final Date lestAvBrukerTidspunkt;
    public final Date lestAvVeilederTidspunkt;
    public final Date venterPaSvarTidspunkt;
    public final Date ferdigbehandletTidspunkt;
    public final Date ubehandletTidspunkt;

    public final Date sisteStatusEndring;
    public final Date opprettetDato;
    public final boolean historisk;

    public final List<HenvendelseData> henvendelser;

    // Aggregerte data
    public final boolean lestAvBruker;
    public final boolean lestAvVeileder;
    public final boolean venterPaSvar;
    public final boolean ferdigbehandlet;

    public boolean erUbehandlet() {
        return !ferdigbehandlet;
    }

}

