package no.nav.fo.veilarbdialog.kvp;


import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class KvpDTO {
    private long kvpId;
    private long serial;
    private String aktorId;
    private String enhet;
    private String opprettetAv;
    private Date opprettetDato;
    private String opprettetBegrunnelse;
    private String avsluttetAv;
    private Date avsluttetDato;
    private String avsluttetBegrunnelse;
}
