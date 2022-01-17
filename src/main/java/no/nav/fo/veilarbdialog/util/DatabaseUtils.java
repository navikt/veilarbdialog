package no.nav.fo.veilarbdialog.util;

import org.apache.commons.lang3.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static java.util.Optional.ofNullable;

public class DatabaseUtils {

    public static ZonedDateTime hentZonedDateTime(ResultSet rs, String kolonneNavn) throws SQLException {
        return  ofNullable(rs.getTimestamp(kolonneNavn))
                .map(Timestamp::toInstant)
                .map(instant -> ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()))
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
           return  null;
       }
    }

}
