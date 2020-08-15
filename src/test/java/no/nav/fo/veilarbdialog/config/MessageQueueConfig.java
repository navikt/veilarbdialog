package no.nav.fo.veilarbdialog.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;

import javax.jms.ConnectionFactory;

@TestConfiguration
public class MessageQueueConfig {

    @MockBean
    private ConnectionFactory connectionFactory;

    @Bean
    public ConnectionFactory connectionFactory() {
        return connectionFactory;
    }

}
