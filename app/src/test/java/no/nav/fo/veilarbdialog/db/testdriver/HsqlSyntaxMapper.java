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
                "alter table HENVENDELSE modify tekst clob not null",
                "alter table HENVENDELSE alter column tekst SET not null"
        );
        map(
                "alter table HENVENDELSE modify sendt timestamp not null",
                "alter table HENVENDELSE alter column sendt SET not null"
        );


    }

    private static void map(String oracleSyntax, String hsqlSyntax) {
        syntaxMap.put(oracleSyntax, hsqlSyntax);
    }

    static String hsqlSyntax(String sql) {
        return syntaxMap.getOrDefault(sql, sql);
    }

}
