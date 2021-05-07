package no.nav.fo.veilarbdialog.domain;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;


@Data
@Accessors(chain = true)
@Builder(toBuilder = true)
public class KladdDTO {
    private String dialogId;
    private String aktivitetId;
    private String overskrift;
    private String tekst;
}
