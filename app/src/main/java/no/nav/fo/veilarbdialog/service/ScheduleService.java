package no.nav.fo.veilarbdialog.service;

import lombok.val;
import no.nav.fo.veilarbdialog.db.dao.VarselDAO;
import no.nav.melding.virksomhet.varsel.v1.varsel.*;
import org.slf4j.Logger;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

import static java.lang.Boolean.TRUE;
import static java.util.UUID.randomUUID;
import static javax.xml.bind.JAXBContext.newInstance;
import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static javax.xml.bind.Marshaller.JAXB_FRAGMENT;
import static org.slf4j.LoggerFactory.getLogger;


@Component
public class ScheduleService {

    private static final Logger LOG = getLogger(ScheduleService.class);

    private static final long GRACE_PERIODE = Long.parseLong(System.getProperty("grace.periode.millis"));
    private static final boolean IS_MASTER = Boolean.parseBoolean(System.getProperty("cluster.ismasternode", "false"));
    private static final String VARSEL_ID = "DittNAV_000007";
    private static final String VARSEL_NAVN = "Aktivitetsplan_nye_henvendelser";

    private static final JAXBContext VARSEL_CONTEXT;

    static {
        try {
            VARSEL_CONTEXT = newInstance(
                    Varsel.class,
                    Varslingstyper.class
            );
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    @Inject
    private VarselDAO varselDAO;

    @Inject
    private JmsTemplate varselQueue;

    @Scheduled(fixedRateString = "${varslingsrate.millis}")
    public void sjekkForSendVarsel() {
        if (IS_MASTER) {
            val aktorer = varselDAO.hentAktorerMedUlesteMeldinger(GRACE_PERIODE);

            LOG.info("Varsler {} brukere", aktorer.size());
            aktorer.forEach(this::sendVarsel);
        }
    }

    private void sendVarsel(String aktorId) {
        varselQueue.send(messageCreator(marshallVarsel(lagNyttVarsel(aktorId))));
        varselDAO.oppdaterSisteVarselForBruker(aktorId);
    }

    private XMLVarsel lagNyttVarsel(String aktoerId) {
        return new XMLVarsel()
                .withMottaker(new XMLAktoerId().withAktoerId(aktoerId))
                .withVarslingstype(new XMLVarslingstyper(VARSEL_ID, null, null));
    }

    public static MessageCreator messageCreator(final String hendelse) {
        return session -> {
            TextMessage msg = session.createTextMessage(hendelse);
            msg.setStringProperty("callId", randomUUID().toString() + VARSEL_NAVN);
            return msg;
        };
    }

    public static String marshallVarsel(Object element) {
        try {
            StringWriter writer = new StringWriter();
            Marshaller marshaller = VARSEL_CONTEXT.createMarshaller();
            marshaller.setProperty(JAXB_FORMATTED_OUTPUT, TRUE);
            marshaller.setProperty(JAXB_FRAGMENT, true);
            marshaller.marshal(element, new StreamResult(writer));
            return writer.toString();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
