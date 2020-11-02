package no.nav.fo.veilarbdialog.domain;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.Date;

import static no.nav.fo.veilarbdialog.domain.AvsenderType.BRUKER;
import static no.nav.fo.veilarbdialog.domain.AvsenderType.VEILEDER;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@Accessors(chain = true)
@With
@ToString(exclude = "tekst")
public class HenvendelseData {

    public final long id;
    public final long dialogId;
    public final String aktivitetId;
    public final Date sendt;
    public final String tekst;
    public final AvsenderType avsenderType;
    public final String avsenderId;

    public final boolean viktig;
    public final boolean lestAvBruker;
    public final boolean lestAvVeileder;

    public final String kontorsperreEnhetId;

    public boolean fraBruker() {
        return avsenderType == BRUKER;
    }

    public boolean fraVeileder() {
        return avsenderType == VEILEDER;
    }

}

