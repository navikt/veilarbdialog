package no.nav.fo.veilarbdialog.util;

import lombok.SneakyThrows;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.GregorianCalendar;

import static java.util.Optional.ofNullable;

public class DateUtils {


    private static final DatatypeFactory datatypeFactory = getDatatypeFactory();

    public static XMLGregorianCalendar xmlCalendar(Date date) {
        return ofNullable(date).map(d -> {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(date);
            return datatypeFactory.newXMLGregorianCalendar(cal);
        }).orElse(null);
    }

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

    @SneakyThrows
    private static DatatypeFactory getDatatypeFactory() {
        return DatatypeFactory.newInstance();
    }
}
