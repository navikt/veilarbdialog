package no.nav.fo.veilarbdialog.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.Wither;

import java.util.Date;
import java.util.List;

import static java.util.Collections.emptyList;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@Accessors(chain = true)
public class DialogData {

    public final long id;
    public final String aktorId;
    public final String overskrift;

    public final List<HenvendelseData> henvendelser;

}

