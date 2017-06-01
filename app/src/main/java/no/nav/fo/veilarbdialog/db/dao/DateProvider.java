package no.nav.fo.veilarbdialog.db.dao;

import org.springframework.stereotype.Component;

@Component
public class DateProvider {

    // Kan mockes i tester
    public String getNow() {
        return "CURRENT_TIMESTAMP";
    }

}
