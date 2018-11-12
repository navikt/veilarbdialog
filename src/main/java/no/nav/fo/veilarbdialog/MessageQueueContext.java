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
        return  queuePingable(varselQueue, "VarselQueue", "Brukes for å sende varsler til bruker om nye dialoger.");
    }

    @Bean
    public Pingable paragraf8VarselQueuePingable(JmsTemplate varselMedHandlingQueue) {
        return  queuePingable(varselMedHandlingQueue, "varselMedHandlingQueue", "Brukes for å sende §8 varsler til bruker.");
    }

    @Bean
    public Pingable stoppRevarselQueuePingable(JmsTemplate stopVarselQueue) {
        return  queuePingable(stopVarselQueue, "stopVarselQueue", "Brukes for å stoppe revarseler av §8 varselr.");
    }

    @Bean
    public Pingable oppgaveHenvendelseQueuePingable(JmsTemplate oppgaveHenvendelseQueue) {
        return  queuePingable(oppgaveHenvendelseQueue, "oppgaveHenvendelseQueue", "Brukes for å sende §8 varsler til bruker.");
    }


    private Pingable queuePingable(JmsTemplate queue, String queueName, String beskrivelse) {
        final PingMetadata metadata = new PingMetadata(
                 queueName + " via " + System.getProperty("mqGateway03.hostname"),
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
    public JmsTemplate varselQueue() throws NamingException {
        return queue(varselDestination());
    }

    @Bean JmsTemplate stopVarselQueue() throws NamingException {
        return queue(stopVarselDestination());
    }

    @Bean JmsTemplate varselMedHandlingQueue() throws NamingException {
        return queue(varselMedHandlingDestination());
    }

    @Bean JmsTemplate oppgaveHenvendelseQueue() throws NamingException {
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
