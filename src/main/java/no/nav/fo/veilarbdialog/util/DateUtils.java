package no.nav.fo.veilarbdialog.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

public class DateUtils {

    public static Date toDate(String value) {
        return Date.from(ZonedDateTime.parse(value).toInstant());
    }

    public static String ISO8601FromDate(Date date, ZoneId zoneId) {
        return ZonedDateTime.ofInstant(date.toInstant(), zoneId).toString();
    }

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
