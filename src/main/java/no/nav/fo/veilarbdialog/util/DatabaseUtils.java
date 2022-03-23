package no.nav.fo.veilarbdialog.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static java.util.Optional.ofNullable;
@Slf4j
public class DatabaseUtils {

    public static ZonedDateTime hentZonedDateTime(ResultSet rs, String kolonneNavn) throws SQLException {
        return  ofNullable(rs.getTimestamp(kolonneNavn))
                .map(Timestamp::toInstant)
                .map(instant -> ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()))
                .orElse(null);
    }

    public static LocalDateTime hentLocalDateTime(ResultSet rs, String kolonneNavn) throws SQLException {
        return  ofNullable(rs.getTimestamp(kolonneNavn))
                .map(Timestamp::toInstant)
                .map(instant -> LocalDateTime.ofInstant(instant, ZoneId.systemDefault()))
                .orElse(null);
    }

    public static UUID hentMaybeUUID(ResultSet rs, String kolonneNavn) throws SQLException {
        String uuid = rs.getString(kolonneNavn);

        if (StringUtils.isEmpty(uuid)) {
            return null;
        }

       try {
        return UUID.fromString(uuid);
       } catch (IllegalArgumentException e) {
           log.warn("Ugyldig UUID: {}. Fortsetter prosessering", uuid);
           return  null;
       }
    }

    public static URL hentMaybeURL(ResultSet rs, String kolonneNavn) throws SQLException {
        String urlString = rs.getString(kolonneNavn);
        if (StringUtils.isEmpty(urlString)) {
            return null;
        }
        try {
            return new URL(urlString);
        } catch (MalformedURLException e) {
            log.warn("Ugyldig URL: {}. Fortsetter prosessering", urlString);
            return null;
        }
    }

}
