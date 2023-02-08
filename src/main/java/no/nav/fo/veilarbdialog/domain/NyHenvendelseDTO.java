package no.nav.fo.veilarbdialog.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.List;

@Data
@Accessors(chain = true)
public class NyHenvendelseDTO {

    private String tekst;
    private String dialogId;
    private String overskrift;
    private String aktivitetId;
    private Boolean venterPaaSvarFraNav;
    private Boolean venterPaaSvarFraBruker;
    private List<Egenskap> egenskaper = Collections.emptyList();

}
