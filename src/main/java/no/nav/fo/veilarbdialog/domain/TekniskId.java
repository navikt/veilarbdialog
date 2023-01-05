package no.nav.fo.veilarbdialog.domain;

import lombok.NonNull;

public class TekniskId extends AktivitetId {
    public TekniskId(String id) {
        super(id);
    }

    public TekniskId(@NonNull Long id) {
        super(id.toString());
    }
}
