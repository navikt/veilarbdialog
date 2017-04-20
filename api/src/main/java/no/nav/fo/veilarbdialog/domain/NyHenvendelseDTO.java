package no.nav.fo.veilarbdialog.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class NyHenvendelseDTO {

    public String tekst;
    public String dialogId;
    public String overskrift;
    public String aktivitetId;

}
