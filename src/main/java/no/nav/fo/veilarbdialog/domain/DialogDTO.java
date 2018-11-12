package no.nav.fo.veilarbdialog.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Accessors(chain = true)
public class DialogDTO {

    public String id;
    public String aktivitetId;
    public String overskrift;
    public String sisteTekst;
    public Date sisteDato;
    public Date opprettetDato;
    public boolean historisk;
    public boolean lest;

    // veileder-felter
    public boolean venterPaSvar;
    public boolean ferdigBehandlet;
    public Date lestAvBrukerTidspunkt;
    public boolean erLestAvBruker;

    public List<HenvendelseDTO> henvendelser = new ArrayList<>();
    public List<Egenskap> egenskaper = new ArrayList<>();
}
