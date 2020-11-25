package no.nav.fo.veilarbdialog.util;

import java.util.Date;

public class DateUtils {

    public static long msSiden(Date date) {
        return new Date().getTime() - date.getTime();
    }

    public static Long nullSafeMsSiden(Date date) {
        if (date == null)
            return null;
        return msSiden(date);
    }

    private DateUtils() {
    }

}
