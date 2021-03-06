package no.nav.fo.veilarbdialog.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@Accessors(chain = true)
public class DialogAktor {

    public final String aktorId;
    public final Date opprettetTidspunkt;
    public final Date sisteEndring;
    public final Date tidspunktEldsteVentende;
    public final Date tidspunktEldsteUbehandlede;

}
