package no.nav.fo.veilarbdialog.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jdbc.repository.config.DialectResolver;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

@Slf4j
public class OracleDialectResolver implements DialectResolver.JdbcDialectProvider {

    @Override
    public Optional<Dialect> getDialect(JdbcOperations jdbcOperations) {
        return Optional.ofNullable(jdbcOperations.execute((ConnectionCallback<Dialect>) OracleDialectResolver::getDialect));
    }

    private static Dialect getDialect(Connection connection)
            throws SQLException {
        if (connection
                .getMetaData()
                .getDatabaseProductName()
                .toLowerCase(Locale.ROOT)
                .contains("oracle")) {
            log.info("Using custom dialect {}", OracleDialect.class.getName());
            System.err.println("FOOBAR");
            return OracleDialect.INSTANCE;
        }
        return null;
    }

}
