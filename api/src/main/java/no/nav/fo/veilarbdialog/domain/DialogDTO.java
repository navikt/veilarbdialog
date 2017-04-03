package no.nav.fo.veilarbdialog.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static java.util.Collections.emptyList;

@Data
@Accessors(chain = true)
public class DialogDTO {

    public String id;
    public String overskrift;
    public String sisteTekst;

    public List<HenvendelseDTO> henvendelser = new ArrayList<>();

}
