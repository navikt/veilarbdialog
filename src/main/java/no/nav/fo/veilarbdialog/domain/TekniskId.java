package no.nav.fo.veilarbdialog.domain;

public class TekniskId extends AktivitetId {
    public TekniskId(String id) {
        super(id);
    }

    public String id() {
        return this.aktivitetsId;
    }
}
