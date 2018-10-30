package no.nav.fo.veilarbdialog;

import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.naming.InitialContext;
import javax.naming.NamingException;


@Configuration
@EnableJms
public class MessageQueueContext {

    @Bean
    public Pingable varselQueuePingable(JmsTemplate varselQueue) {
        final PingMetadata metadata = new PingMetadata(
                "VarselQueue via " + System.getProperty("mqGateway03.hostname"),
                "Brukes for å sende varsler til bruker om nye dialoger.",
                true
        );
        return () -> {
            try {
                varselQueue.getConnectionFactory().createConnection().close();
            } catch (JMSException e) {
                return Ping.feilet(metadata, "Kunne ikke opprette connection", e);
            }
            return Ping.lyktes(metadata);
        };
    }

    @Bean
    public JmsTemplate varselQueue() throws NamingException {
        return queue(varselDestination());
    }

    @Bean JmsTemplate stopVarselQueue() throws NamingException {
        return queue(stopVarselDestination());
    }

    @Bean JmsTemplate varselMedHandlingQueue() throws NamingException {
        return queue(varselMedHandlingDestination());
    }

    @Bean JmsTemplate oppgaveHenvendelseQue() throws NamingException {
        return queue(oppgaveHenvendelseDestinasjon());
    }

    private JmsTemplate queue(Destination destination) throws NamingException {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(connectionFactory());
        jmsTemplate.setDefaultDestination(destination);
        return jmsTemplate;
    }

    private ConnectionFactory connectionFactory() throws NamingException {
        return (ConnectionFactory) new InitialContext().lookup("java:jboss/mqConnectionFactory");
    }

    private Destination varselDestination() throws NamingException {
        return (Destination) new InitialContext().lookup("java:/jboss/jms/VARSELPRODUKSJON.VARSLINGER");
    }

    private Destination stopVarselDestination() throws NamingException {
        return (Destination) new InitialContext().lookup("java:/jboss/jms/VARSELPRODUKSJON.STOPP_VARSEL_UTSENDING");
    }

    private Destination varselMedHandlingDestination() throws NamingException {
        return (Destination) new InitialContext().lookup("java:/jboss/jms/VARSELPRODUKSJON.BEST_VARSEL_M_HANDLING");
    }

    private Destination oppgaveHenvendelseDestinasjon() throws  NamingException {
        return (Destination) new InitialContext().lookup("java:/jboss/jms/henvendelse_OPPGAVE.HENVENDELSE");
    }

}
