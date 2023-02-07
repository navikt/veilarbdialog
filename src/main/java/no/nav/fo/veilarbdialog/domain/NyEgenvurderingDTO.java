package no.nav.fo.veilarbdialog.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.List;

@Data
@Accessors(chain = true)
public class NyEgenvurderingDTO {

    private String tekst;
    private String dialogId;
    private String overskrift;
    private String aktivitetId;
    private boolean venterPaaSvarFraNav;
    private List<Egenskap> egenskaper = Collections.emptyList();

    public NyHenvendelseDTO toNyHenvendelseDto() {
        return new NyHenvendelseDTO()
                .setTekst(tekst)
                .setDialogId(dialogId)
                .setAktivitetId(aktivitetId)
                .setEgenskaper(egenskaper);
    }
}
