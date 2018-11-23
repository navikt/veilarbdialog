package no.nav.fo.veilarbdialog.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.List;

@Data
@Accessors(chain = true)
public class NyHenvendelseDTO {

    public String tekst;
    public String dialogId;
    public String overskrift;
    public String aktivitetId;
    public List<Egenskap> egenskaper = Collections.emptyList();

}
