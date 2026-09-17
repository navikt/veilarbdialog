package no.nav.fo.veilarbdialog.db.jdbc;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

/**
 * Type conversions between the persistence layer (which uses {@link LocalDateTime},
 * the native mapping for Postgres {@code timestamp} in Spring Data JDBC) and the
 * domain model (which uses {@link Date} and {@link ZonedDateTime}).
 *
 * <p>All conversions use the system default zone, matching the previous
 * {@code NamedParameterJdbcTemplate} behaviour (see {@code DatabaseUtils}).
 */
public final class JdbcConverters {

    private JdbcConverters() {
    }

    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public static Date toDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDateTime toLocalDateTime(ZonedDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDateTime();
    }

    public static ZonedDateTime toZonedDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneId.systemDefault());
    }

    public static String toString(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return uuid.toString();
    }

    public static UUID parseUuidOrNull(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static URL parseUrlOrNull(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static Integer toInt(boolean value) {
        return value ? 1 : 0;
    }

    public static boolean toBoolean(Integer value) {
        return value != null && value != 0;
    }
}
