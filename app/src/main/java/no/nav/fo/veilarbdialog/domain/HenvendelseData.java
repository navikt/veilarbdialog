package no.nav.fo.veilarbdialog.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.Wither;

import java.util.Date;

import static no.nav.fo.veilarbdialog.domain.AvsenderType.BRUKER;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@Accessors(chain = true)
@Wither
public class HenvendelseData {

    public final long dialogId;
    public final String aktivitetId;
    public final Date sendt;
    public final String tekst;
    public final AvsenderType avsenderType;
    public final String avsenderId;

    public final boolean lestAvBruker;
    public final boolean lestAvVeileder;

    public boolean fraBruker() {
        return avsenderType == BRUKER;
    }

}

