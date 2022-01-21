package no.nav.fo.veilarbdialog.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Accessors(chain = true)
public class DialogDTO {

    private String id;
    private String aktivitetId;
    private String overskrift;
    private String sisteTekst;
    private Date sisteDato;
    private Date opprettetDato;
    private boolean historisk;
    private boolean lest; // lest av brukertype som gjør kall

    // veileder-felter
    private boolean venterPaSvar;
    private boolean ferdigBehandlet;
    private Date lestAvBrukerTidspunkt;
    private boolean erLestAvBruker;

    private List<HenvendelseDTO> henvendelser = new ArrayList<>();
    private List<Egenskap> egenskaper = new ArrayList<>();
}
