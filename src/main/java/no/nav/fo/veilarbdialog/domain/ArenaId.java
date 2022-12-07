package no.nav.fo.veilarbdialog.domain;


import java.util.Optional;

public record ArenaId(String id) {

    public ArenaId(String id) {
        if (id.startsWith("ARENA")) {
            this.id = id;
        } else {
            this.id = "ARENA" + id;
        }
    }

    public static Optional<ArenaId> of(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.of(new ArenaId(id));
    }

}


