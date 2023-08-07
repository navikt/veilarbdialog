package no.nav.fo.veilarbdialog.db.dao;

public class AktorIdProvider {

    private static int nr = 1000;


    public static String getNext() {
        return nr++ + "";
    }
}
