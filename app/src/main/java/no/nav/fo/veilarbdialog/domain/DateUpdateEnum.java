package no.nav.fo.veilarbdialog.domain;

import java.util.function.Function;

public enum DateUpdateEnum {
    KEEP(s -> s + " = " + s),
    NOW(s -> s + " = CURRENT_TIMESTAMP"),
    NULL(s -> s + " = NULL"),
    NYESTE_HENVENDELSE(s -> s + " = (SELECT max(SENDT) from HENVENDELSE where HENVENDELSE.DIALOG_ID = DIALOG.DIALOG_ID)");

    private Function<String, String> fn;

    DateUpdateEnum(Function<String, String> fn) {
        this.fn = fn;
    }

    public String toSQL(String kolonne) {
        return fn.apply(kolonne);
    }
}
