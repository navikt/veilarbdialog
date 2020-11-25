package no.nav.fo.veilarbdialog.db.dao;

import org.springframework.stereotype.Component;

import java.util.function.Supplier;

// TODO: Refactor and remove.
@Component
public class DateProvider {

    private static final Supplier<String> provider = () -> "CURRENT_TIMESTAMP";
    public String getNow() {
        return provider.get();
    }

}
