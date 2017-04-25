package no.nav.fo.veilarbdialog.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class OppdaterDialogDTO {

    public String id;
    public boolean kreverSvar;
    public boolean behandlet;

}
