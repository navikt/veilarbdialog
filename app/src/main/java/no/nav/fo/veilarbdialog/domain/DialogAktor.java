package no.nav.fo.veilarbdialog.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@Accessors(chain = true)
public class DialogAktor {

    public final String aktorId;
    public final Date sisteEndring;
    public final boolean venterPaSvar;
    public final boolean ubehandlet;

}
