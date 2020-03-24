package no.nav.fo.veilarbdialog.domain;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;


@Data
@Accessors(chain = true)
@Builder(toBuilder = true)
public class KladdDTO {
    public String dialogId;
    public String aktivitetId;
    public String overskrift;
    public String tekst;
}
