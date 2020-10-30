package no.nav.fo.veilarbdialog.db.dao;

import org.springframework.stereotype.Component;

import java.sql.Timestamp;

/**
 * H2 friendly.
 */
@Component
public class DateProvider {

    public String getNow() {
        return "CURRENT_TIMESTAMP";
    }

}
