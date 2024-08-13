package no.nav.fo.veilarbdialog.config;

import io.getunleash.Unleash;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.token_client.client.TokenXOnBehalfOfTokenClient;
import no.nav.fo.veilarbdialog.db.DataSourceConfig;
import no.nav.fo.veilarbdialog.db.dao.LocalDatabaseSingleton;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
public class ApplicationTestConfig {

    @Bean
    public DataSource dataSource() throws IOException {
        var db = LocalDatabaseSingleton.INSTANCE.getPostgres();
        return db;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public String pdlUrl(Environment environment) {
        return environment.getRequiredProperty("application.pdl.api.url");
    }

    @Bean
    public AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient() {
        AzureAdMachineToMachineTokenClient tokenClient = mock(AzureAdMachineToMachineTokenClient.class);
        when(tokenClient.createMachineToMachineToken(any())).thenReturn("mockMachineToMachineToken");
        return tokenClient;
    }

    @Bean
    public TokenXOnBehalfOfTokenClient tokenXOnBehalfOfTokenClient() {
        TokenXOnBehalfOfTokenClient client = mock(TokenXOnBehalfOfTokenClient.class);
        when(client.exchangeOnBehalfOfToken(any(), any())).thenReturn("mockMachineToMachineToken");
        return client;
    }

    @Bean
    public AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient() {
        AzureAdOnBehalfOfTokenClient tokenClient = mock(AzureAdOnBehalfOfTokenClient.class);
        when(tokenClient.exchangeOnBehalfOfToken(any(), any())).thenReturn("mockMachineToMachineToken");
        return tokenClient;
    }

    @Bean
    Unleash unleash() {
        return mock(Unleash.class);
    }

}
