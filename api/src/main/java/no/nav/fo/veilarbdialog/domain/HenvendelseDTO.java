package no.nav.fo.veilarbdialog.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class HenvendelseDTO {

    public String dialogId;
    public Date sendt;
    public String tekst;

}
