package no.nav.fo.veilarbdialog.db.dao;

import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for Spring Data JDBC slice tests. Concrete tests add their DAO
 * under test via {@code @Import}; repositories are auto-detected. The shared
 * embedded Postgres database is used as-is (no test-database replacement).
 *
 * <p>The test-managed transaction is disabled ({@code NOT_SUPPORTED}) so each
 * statement auto-commits, exactly like the pre-migration tests: Postgres
 * {@code CURRENT_TIMESTAMP} then reflects statement time rather than a
 * frozen transaction-start time.
 */
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(JdbcTestConfig.class)
public abstract class BaseDAOTest {
}
