package no.nav.fo.veilarbdialog;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.naming.InitialContext;
import javax.naming.NamingException;


@Configuration
@EnableJms
public class MessageQueueContext {

    @Bean
    public JmsTemplate varselQueue() throws NamingException {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(connectionFactory());
        jmsTemplate.setDefaultDestination(varselDestination());
        return jmsTemplate;
    }

    private ConnectionFactory connectionFactory() throws NamingException {
        return (ConnectionFactory) new InitialContext().lookup("java:jboss/mqConnectionFactory");
    }

    private Destination varselDestination() throws NamingException {
        return (Destination) new InitialContext().lookup("java:/jboss/jms/VARSELPRODUKSJON.VARSLINGER");
    }

}
