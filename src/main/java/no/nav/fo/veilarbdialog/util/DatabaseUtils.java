package no.nav.fo.veilarbdialog.util;

import lombok.SneakyThrows;
import oracle.sql.ROWID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.support.KeyHolder;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static java.util.Optional.ofNullable;

public class DatabaseUtils {

    public static ZonedDateTime hentZonedDateTime(ResultSet rs, String kolonneNavn) throws SQLException {
        return ofNullable(rs.getTimestamp(kolonneNavn))
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
            return null;
        }
    }

    @SneakyThrows
    public static long getGeneratedKey(KeyHolder keyHolder) {
        Object generatedKey = keyHolder.getKeyAs(Object.class);

        if (generatedKey == null) {
            throw new DataAccessResourceFailureException("Generated key not present");
        }

        if (generatedKey instanceof BigDecimal key) {
            // Used by H2
            return key.longValue();
        } else if (generatedKey instanceof ROWID key) {
            // Used by Oracle
            return key.longValue();
        } else {
            throw new DataAccessResourceFailureException("Unknown generated key type");
        }
    }

}
