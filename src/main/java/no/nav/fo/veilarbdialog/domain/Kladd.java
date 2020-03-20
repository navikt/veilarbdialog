package no.nav.fo.veilarbdialog.domain;


import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;


@Data
@Accessors(chain = true)
@Builder(toBuilder = true)
public class Kladd {
    public Long dialogId;
    public String aktivitetId;
    public String aktorId;
    public String overskrift;
    public String tekst;
    public String lagtInnAv;
}
