package no.nav.fo.veilarbdialog.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class HenvendelseDTO {

    private String id;
    private String dialogId;
    private Avsender avsender;
    private String avsenderId;
    private Date sendt;
    private boolean lest;
    private boolean viktig;
    private String tekst;

}
