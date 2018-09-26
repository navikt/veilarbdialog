package no.nav.fo.veilarbdialog.util;

import lombok.SneakyThrows;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;

import javax.naming.InitialContext;

public class VarselMock {

    @SneakyThrows
    public static void init() {
        if ("true".equals(System.getProperty("lokal-activemq"))) {
            final BrokerService broker = new BrokerService();
            broker.getSystemUsage().getTempUsage().setLimit(100 * 1024 * 1024 * 100);
            broker.getSystemUsage().getStoreUsage().setLimit(100 * 1024 * 1024 * 100);
            broker.addConnector("tcp://localhost:61616");
            broker.start();
        }

        InitialContext ctx = new InitialContext();
        ctx.createSubcontext("java:/");
        ctx.createSubcontext("java:/jboss/");
        ctx.createSubcontext("java:/jboss/jms/");

        ctx.bind("java:/jboss/jms/VARSELPRODUKSJON.VARSLINGER", new ActiveMQQueue("dialogvarsel"));
        ctx.bind("java:/jboss/jms/VARSELPRODUKSJON.STOPP_VARSEL_UTSENDING", new ActiveMQQueue("stopp_varsel"));
        ctx.bind("java:jboss/mqConnectionFactory", new ActiveMQConnectionFactory("tcp://localhost:61616"));
    }
}
