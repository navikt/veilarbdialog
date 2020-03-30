package no.nav.fo.veilarbdialog.domain;


import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.Wither;


@Data
@Accessors(chain = true)
@Builder(toBuilder = true)
@Wither
public class Kladd {
    public String dialogId;
    public String aktivitetId;
    public String aktorId;
    public String overskrift;
    public String tekst;
    public String lagtInnAv;
}
