package no.nav.fo.veilarbdialog.domain;


import lombok.Builder;
import lombok.Data;
import lombok.With;
import lombok.experimental.Accessors;


@Data
@Accessors(chain = true)
@Builder(toBuilder = true)
@With
public class Kladd {
    public String dialogId;
    public String aktivitetId;
    public String aktorId;
    public String overskrift;
    public String tekst;
    public String lagtInnAv;
}
