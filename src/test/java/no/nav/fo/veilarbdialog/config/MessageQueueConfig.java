package no.nav.fo.veilarbdialog.config;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbdialog.TestApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.EnableJms;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

@TestConfiguration
@EnableJms
@Slf4j
public class MessageQueueConfig {

    @Value("${application.mq.hostname}")
    private String mqGatewayHostname;

    @Value("${application.mq.name}")
    private String mqGatewayName;

    @Value("${application.mq.userid}")
    private String mqUserId;

    @Value("${application.mq.channel:}")
    private String mqChannel;

    @MockBean
    private ConnectionFactory connectionFactory;

    @Bean
    @Profile("!local")
    public ConnectionFactory mockedConnectionFactory() {
        return connectionFactory;
    }

    @Bean
    @Profile("local")
    public ConnectionFactory connectionFactory()
            throws JMSException {

        JmsConnectionFactory connectionFactory = JmsFactoryFactory
                .getInstance(WMQConstants.WMQ_PROVIDER)
                .createConnectionFactory();
        connectionFactory.setStringProperty(WMQConstants.WMQ_HOST_NAME, mqGatewayHostname);
        String port = TestApplication.IBM_MQ.getFirstMappedPort().toString();
        log.info("Using assigned port {} from Testcontainers for IBM MQ", port);
        connectionFactory.setStringProperty(WMQConstants.WMQ_PORT, port);
        connectionFactory.setStringProperty(WMQConstants.WMQ_CHANNEL, mqChannel);
        connectionFactory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
        connectionFactory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, mqGatewayName);
        connectionFactory.setStringProperty(WMQConstants.USERID, mqUserId);
        return connectionFactory;

    }

}