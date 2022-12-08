package no.nav.fo.veilarbdialog.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public
class AktivitetId {
    protected final String aktivitetsId;

    public String getId() {
        return aktivitetsId;
    }

    public static AktivitetId of(String id) {
        if (id == null) return null;
        if (id.startsWith("ARENA")) {
            return new Arenaid(id);
        } else {
            return new TekniskId(id);
        }

    }
}

