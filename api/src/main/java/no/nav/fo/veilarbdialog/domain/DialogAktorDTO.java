package no.nav.fo.veilarbdialog.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class DialogAktorDTO implements Comparable<DialogAktorDTO> {

    public String aktorId;
    public Date sisteEndring;
    public boolean venterPaSvar;
    public boolean harUbehandlet;

    @Override
    public int compareTo(DialogAktorDTO o) {
        return o.sisteEndring.compareTo(this.sisteEndring);
    }
}
