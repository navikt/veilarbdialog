package no.nav.fo.veilarbdialog.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Accessors(chain = true)
public class DialogAktorDTO {

    public String aktorId;
    public Date sisteEndring;
    public boolean venterPaSvar;
    public boolean ubehandler;

}
