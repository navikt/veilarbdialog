package no.nav.fo.veilarbdialog;


import io.restassured.RestAssured;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.fo.veilarbdialog.util.DialogTestService;
import no.nav.fo.veilarbdialog.util.KafkaTestService;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
@Sql(
        scripts = "/db/testdata/slett_alle_dialoger.sql",
        executionPhase = BEFORE_TEST_METHOD
)
public abstract class SpringBootTestBase {
    @Autowired
    protected KafkaTestService kafkaTestService;

    @Autowired
    protected DialogTestService dialogTestService;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private LockProvider lockProvider;

    @LocalServerPort
    protected int port;

    @Before
    public void setup() {
        RestAssured.port = port;
        JdbcTemplateLockProvider l = (JdbcTemplateLockProvider) lockProvider;
        l.clearCache();
    }
}
