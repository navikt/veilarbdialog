package no.nav.fo.veilarbdialog;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.getunleash.Unleash;
import io.restassured.RestAssured;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.util.DialogTestService;
import no.nav.fo.veilarbdialog.util.KafkaTestService;
import no.nav.poao_tilgang.poao_tilgang_test_wiremock.PoaoTilgangWiremock;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@AutoConfigureWireMock(port = 0)
@WireMockTest
@Sql(
        scripts = "/db/testdata/slett_alle_dialoger.sql",
        executionPhase = BEFORE_TEST_METHOD
)
public abstract class SpringBootTestBase {
    private static final PoaoTilgangWiremock poaoTilgangWiremock = new PoaoTilgangWiremock(0, "", MockNavService.NAV_CONTEXT);


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

    @Autowired
    protected Unleash unleash;

    @LocalServerPort
    protected int port;

    @BeforeEach
    public void setup() {
        RestAssured.port = port;
        JdbcTemplateLockProvider l = (JdbcTemplateLockProvider) lockProvider;
        l.clearCache();
    }

    @DynamicPropertySource
    public static void tilgangskotroll(DynamicPropertyRegistry registry) {
        registry.add("application.poao_tilgang.url", () -> poaoTilgangWiremock.getWireMockServer().baseUrl());
    }
}
