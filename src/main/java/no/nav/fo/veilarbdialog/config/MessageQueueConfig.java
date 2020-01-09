package no.nav.fo.veilarbdialog.config;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import java.util.UUID;

import static no.nav.fo.veilarbdialog.config.ApplicationConfig.*;
import static no.nav.sbl.util.EnvironmentUtils.*;

@Configuration
@EnableJms
public class MessageQueueConfig {

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
                queueName + " via " + getRequiredProperty(MQGATEWAY03_HOSTNAME_PROPERTY),
                beskrivelse,
                false
        );
        return () -> {
            try {
                queue.getConnectionFactory().createConnection().close();
            } catch (JMSException e) {
                return Ping.feilet(metadata, "Kunne ikke opprette connection", e);
            }
            return Ping.lyktes(metadata);
        };
    }

    @Bean
    public JmsTemplate varselQueue(ConnectionFactory connectionFactory) {
        return queue(connectionFactory, getRequiredProperty(VARSELPRODUKSJON_VARSLINGER_QUEUENAME_PROPERTY));
    }

    @Bean
    JmsTemplate stopVarselQueue(ConnectionFactory connectionFactory) {
        return queue(connectionFactory, getRequiredProperty(VARSELPRODUKSJON_STOPP_VARSEL_UTSENDING_QUEUENAME_PROPERTY));
    }

    @Bean
    JmsTemplate varselMedHandlingQueue(ConnectionFactory connectionFactory) {
        return queue(connectionFactory, getRequiredProperty(VARSELPRODUKSJON_BEST_VARSEL_M_HANDLING_QUEUENAME_PROPERTY));
    }

    @Bean
    JmsTemplate oppgaveHenvendelseQueue(ConnectionFactory connectionFactory) {
        return queue(connectionFactory, getRequiredProperty(HENVENDELSE_OPPGAVE_HENVENDELSE_QUEUENAME_PROPERTY));
    }

    private JmsTemplate queue(ConnectionFactory connectionFactory, String queueName) {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(connectionFactory);

        JMSContext context = connectionFactory.createContext();
        jmsTemplate.setDefaultDestination(context.createQueue(queueName));

        return jmsTemplate;
    }

    @Bean
    public ConnectionFactory connectionFactory() throws JMSException {
        JmsFactoryFactory jmsFactoryFactory = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
        JmsConnectionFactory connectionFactory = jmsFactoryFactory.createConnectionFactory();

        String env = requireNamespace().equals("default") ? "p" : requireNamespace();

        connectionFactory.setStringProperty(WMQConstants.WMQ_HOST_NAME, getRequiredProperty(MQGATEWAY03_HOSTNAME_PROPERTY));
        connectionFactory.setStringProperty(WMQConstants.WMQ_PORT, getRequiredProperty(MQGATEWAY03_PORT_PROPERTY));
        connectionFactory.setStringProperty(WMQConstants.WMQ_CHANNEL, String.format("%s_%s", env, requireApplicationName()).toUpperCase());
        connectionFactory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
        connectionFactory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, getRequiredProperty(MQGATEWAY03_NAME_PROPERTY));
        connectionFactory.setStringProperty(WMQConstants.USERID, "srvappserver");

        return connectionFactory;
    }
}
