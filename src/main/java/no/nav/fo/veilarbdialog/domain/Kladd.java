package no.nav.fo.veilarbdialog.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.With;
import lombok.experimental.Accessors;


@Data
@AllArgsConstructor
@Accessors(chain = true)
@Builder(toBuilder = true)
@With
public class Kladd {
    private String dialogId;
    private String aktivitetId;
    private String aktorId;
    private String overskrift;
    private String tekst;
    private String lagtInnAv;
}
