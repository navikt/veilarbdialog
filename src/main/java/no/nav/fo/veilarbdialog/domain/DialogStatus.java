package no.nav.fo.veilarbdialog.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@Accessors(chain = true)
public class DialogStatus {

    public final long dialogId;
    public final boolean venterPaSvar;
    public final boolean ferdigbehandlet;

}

