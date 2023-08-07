package no.nav.fo.veilarbdialog.db.dao;

import org.flywaydb.core.Flyway;
import org.h2.tools.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;

public abstract class BaseDAOTest {

    static JdbcTemplate jdbc;
    private static Server h2Server;
    @BeforeAll
    public static void setupH2() throws SQLException {
        // Start H2 TCP server
        h2Server = Server.createTcpServer("-tcpAllowOthers").start();
        System.out.println("H2 server started and listening on port " + h2Server.getPort());

        // Create and configure data source
        DataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:veilarbdialog-local;DB_CLOSE_DELAY=-1;MODE=Oracle;");
        jdbc = new JdbcTemplate(dataSource);

        // Apply Flyway migrations
        Flyway flyway = Flyway.configure().dataSource(dataSource).load();
        flyway.migrate();
    }

    @AfterAll
    public static void tearDownH2() throws SQLException {
        // Stop H2 server
        if (h2Server != null) {
            h2Server.stop();
            System.out.println("H2 server stopped.");
        }
    }

}
