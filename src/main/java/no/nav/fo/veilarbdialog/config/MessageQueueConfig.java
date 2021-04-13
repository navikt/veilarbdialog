package no.nav.fo.veilarbdialog.config;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import lombok.extern.slf4j.Slf4j;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;

import javax.annotation.PostConstruct;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import java.util.Optional;
import java.util.UUID;

import static com.ibm.msg.client.jms.JmsConstants.WMQ_PROVIDER;
import static com.ibm.msg.client.wmq.common.CommonConstants.*;

@Configuration
@EnableJms
@Slf4j
public class MessageQueueConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${application.namespace}")
    private String namespace;

    @Value("${application.mq.hostname}")
    private String mqGatewayHostname;

    @Value("${application.mq.port}")
    private String mqGatewayPort;

    @Value("${application.mq.name}")
    private String mqGatewayName;

    @Value("${application.mq.userid}")
    private String mqUserId;

    /**
     * Optional. If not configured, the value {@code <namespace>_<applicationName>} (uppercase) will be used automatically.
     *
     * @see #postConstruct()
     */
    @Value("${application.mq.channel:}")
    private String mqChannel;

    @Value("${application.mq.queues.varslinger}")
    private String queueNameVarslinger;

    @Value("${application.mq.queues.stoppVarslinger}")
    private String queueNameStoppVarslinger;

    @Value("${application.mq.queues.varselHandling}")
    private String queueNameVarselHandling;

    @Value("${application.mq.queues.oppgaveHenvendelse}")
    private String queueNameOppgaveHenvendelse;

    @PostConstruct
    private void postConstruct() {

        // Fallback to autoconfigured channel name.
        if (mqChannel.isEmpty()) {
            String env = namespace.equals("default") ? "p" : namespace;
            mqChannel = String.format("%s_%s", env, applicationName).toUpperCase();
            log.info("Using implicit channel {}", mqChannel);
        } else {
            log.info("Using explicit channel {}", mqChannel);
        }

    }

    @Bean
    public Pingable varselQueuePingable(JmsTemplate varselQueue) {
        return queuePingable(varselQueue, "VarselQueue", "Brukes for å sende varsler til bruker om nye dialoger.");
    }

    @Bean
    public Pingable paragraf8VarselQueuePingable(JmsTemplate varselMedHandlingQueue) {
        return queuePingable(varselMedHandlingQueue, "varselMedHandlingQueue", "Brukes for å sende §8 varsler til bruker.");
    }

    @Bean
    public Pingable stoppRevarselQueuePingable(JmsTemplate stopVarselQueue) {
        return queuePingable(stopVarselQueue, "stopVarselQueue", "Brukes for å stoppe revarseler av §8 varselr.");
    }

    @Bean
    public Pingable oppgaveHenvendelseQueuePingable(JmsTemplate oppgaveHenvendelseQueue) {
        return queuePingable(oppgaveHenvendelseQueue, "oppgaveHenvendelseQueue", "Brukes for å sende §8 varsler til bruker.");
    }

    private Pingable queuePingable(JmsTemplate queue, String queueName, String beskrivelse) {
        final PingMetadata metadata = new PingMetadata(
                UUID.randomUUID().toString(),
                queueName + " via " + mqGatewayHostname,
                beskrivelse,
                false
        );
        return () -> {
            try {
                Optional
                        .ofNullable(queue.getConnectionFactory())
                        .orElseThrow(() -> new JMSException("Unable to get connection factory"))
                        .createConnection()
                        .close();
            } catch (JMSException e) {
                return Ping.feilet(metadata, "Kunne ikke opprette connection", e);
            }
            return Ping.lyktes(metadata);
        };
    }

    @Bean
    JMSContext jmsContext(ConnectionFactory factory) {
        return factory.createContext();
    }

    @Bean
    public JmsTemplate varselQueue(ConnectionFactory factory, JMSContext context) {
        return queue(factory, context, queueNameVarslinger);
    }

    @Bean
    JmsTemplate stopVarselQueue(ConnectionFactory factory, JMSContext context) {
        return queue(factory, context, queueNameStoppVarslinger);
    }

    @Bean
    JmsTemplate varselMedHandlingQueue(ConnectionFactory factory, JMSContext context) {
        return queue(factory, context, queueNameVarselHandling);
    }

    @Bean
    JmsTemplate oppgaveHenvendelseQueue(ConnectionFactory factory, JMSContext context) {
        return queue(factory, context, queueNameOppgaveHenvendelse);
    }

    private JmsTemplate queue(ConnectionFactory factory, JMSContext context, String queueName) {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(factory);
        jmsTemplate.setDefaultDestination(context.createQueue(queueName));
        return jmsTemplate;
    }

    @Bean
    public ConnectionFactory connectionFactory()
            throws JMSException {

        JmsConnectionFactory connectionFactory = JmsFactoryFactory
                .getInstance(WMQ_PROVIDER)
                .createConnectionFactory();
        connectionFactory.setStringProperty(WMQ_HOST_NAME, mqGatewayHostname);
        connectionFactory.setStringProperty(WMQ_PORT, mqGatewayPort);
        connectionFactory.setStringProperty(WMQ_CHANNEL, mqChannel);
        connectionFactory.setIntProperty(WMQ_CONNECTION_MODE, WMQ_CM_CLIENT);
        connectionFactory.setStringProperty(WMQ_QUEUE_MANAGER, mqGatewayName);
        connectionFactory.setStringProperty(USERID, mqUserId);
        return connectionFactory;

    }

}
