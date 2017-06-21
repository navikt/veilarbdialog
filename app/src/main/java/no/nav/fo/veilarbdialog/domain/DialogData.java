package no.nav.fo.veilarbdialog.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@Accessors(chain = true)
public class DialogData {

    public final long id;
    public final String aktorId;
    public final String overskrift;
    public final String aktivitetId;

    public final boolean lestAvBruker;
    public final boolean lestAvVeileder;
    public final boolean venterPaSvar;
    public final boolean ferdigbehandlet;

    public final Date lestAvBrukerTidspunkt;
    public final Date sisteStatusEndring;
    public final Date historiskDato;

    public final List<HenvendelseData> henvendelser;

}

