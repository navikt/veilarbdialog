package no.nav.fo.veilarbdialog.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLAktoerId;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarsel;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarslingstyper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;

import static java.util.UUID.randomUUID;
import static no.nav.fo.veilarbdialog.util.MessageQueueUtils.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceMeldingService {

    public static final JAXBContext VARSEL_CONTEXT = jaxbContext(XMLVarsel.class, XMLVarslingstyper.class);

    private static final String VARSEL_ID = "DittNAV_000007";
    private static final String VARSEL_NAVN = "Aktivitetsplan_nye_henvendelser";

    private final JmsTemplate varselQueue;

    @SneakyThrows
    void sendVarsel(String aktorId) {
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
