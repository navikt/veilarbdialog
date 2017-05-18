package no.nav.fo.veilarbdialog.db.testdriver;

import java.util.HashMap;
import java.util.Map;

/*
"Fattigmanns-løsning" for å kunne bruke hsql lokalt med oracle syntax
*/
class HsqlSyntaxMapper {

    private static final Map<String, String> syntaxMap = new HashMap<>();

    static {
        map(
                "alter table HENVENDELSE modify sendt timestamp not null",
                "alter table HENVENDELSE alter column sendt SET not null"
        );

        map(
                "ALTER TABLE DIALOG RENAME COLUMN lest_av_bruker TO lest_av_bruker_tid",
                "ALTER TABLE DIALOG ALTER COLUMN lest_av_bruker RENAME TO lest_av_bruker_tid"
        );

        map(
                "ALTER TABLE DIALOG RENAME COLUMN lest_av_veileder TO lest_av_veileder_tid",
                "ALTER TABLE DIALOG ALTER COLUMN lest_av_veileder RENAME TO lest_av_veileder_tid"
        );

    }

    private static void map(String oracleSyntax, String hsqlSyntax) {
        syntaxMap.put(oracleSyntax, hsqlSyntax);
    }

    static String hsqlSyntax(String sql) {
        return syntaxMap.getOrDefault(sql, sql);
    }

}
