package no.nav.fo.veilarbdialog.domain;

import java.util.function.Function;

public enum BooleanUpdateEnum {
    KEEP (s -> s + " = " + s),
    FALSE (s -> s + " = 0"),
    TRUE (s -> s + " = 1");

    private Function<String, String> fn;

    BooleanUpdateEnum(Function<String, String> fn) {
        this.fn = fn;
    }

    public String toSQL(String kolonne) {
        return fn.apply(kolonne);
    }
}
