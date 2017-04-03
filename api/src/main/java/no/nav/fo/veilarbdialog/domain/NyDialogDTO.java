package no.nav.fo.veilarbdialog.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class NyDialogDTO {

    public String overskrift;
    public String tekst;

}
