package no.nav.fo.veilarbdialog.service;

import no.nav.melding.virksomhet.varsel.v1.varsel.XMLAktoerId;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarsel;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarslingstyper;
import org.slf4j.Logger;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import static java.util.UUID.randomUUID;
import static javax.xml.bind.JAXBContext.newInstance;
import static no.nav.fo.veilarbdialog.util.Utils.marshall;
import static no.nav.fo.veilarbdialog.util.Utils.messageCreator;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class ServiceMeldingService {

    @Inject
    private JmsTemplate varselQueue;

    private static final Logger LOG = getLogger(ServiceMeldingService.class);

    private static final String VARSEL_ID = "DittNAV_000007";
    private static final String VARSEL_NAVN = "Aktivitetsplan_nye_henvendelser";

    private static final JAXBContext VARSEL_CONTEXT;

    static {
        try {
            VARSEL_CONTEXT = newInstance(
                    XMLVarsel.class,
                    XMLVarslingstyper.class
            );
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendVarsel(String aktorId) {
        XMLVarsel xmlVarsel = lagNyttVarsel(aktorId);
        String message = marshall(xmlVarsel, VARSEL_CONTEXT);
        MessageCreator messageCreator = messageCreator(message, randomUUID().toString() + VARSEL_NAVN);

        varselQueue.send(messageCreator);
    }

    static XMLVarsel lagNyttVarsel(String aktoerId) {
        return new XMLVarsel()
                .withMottaker(new XMLAktoerId().withAktoerId(aktoerId))
                .withVarslingstype(new XMLVarslingstyper(VARSEL_ID, null, null));
    }
}
