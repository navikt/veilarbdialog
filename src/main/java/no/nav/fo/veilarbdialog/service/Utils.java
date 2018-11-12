package no.nav.fo.veilarbdialog.service;


import org.springframework.jms.core.MessageCreator;

import javax.jms.TextMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

import static java.lang.Boolean.TRUE;
import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static javax.xml.bind.Marshaller.JAXB_FRAGMENT;

public class Utils {

    static MessageCreator messageCreator(final String hendelse, String uuid) {
        return session -> {
            TextMessage msg = session.createTextMessage(hendelse);
            msg.setStringProperty("callId", uuid);
            return msg;
        };
    }

    static String marshall(Object element, JAXBContext jaxbContext) {
        try {
            StringWriter writer = new StringWriter();
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(JAXB_FORMATTED_OUTPUT, TRUE);
            marshaller.setProperty(JAXB_FRAGMENT, true);
            marshaller.marshal(element, new StreamResult(writer));
            return writer.toString();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

}
