package no.nav.fo.veilarbdialog.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.fo.veilarbdialog.jms.NamespacedXmlVarsel;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarslingstyper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

import static java.util.UUID.randomUUID;
import static no.nav.fo.veilarbdialog.util.MessageQueueUtils.messageCreator;

@Component
@RequiredArgsConstructor
public class ServiceMeldingService {

    private static final String VARSEL_ID = "DittNAV_000007";
    private static final String VARSEL_NAVN = "Aktivitetsplan_nye_henvendelser";

    private final JmsTemplate varselQueue;
    private final XmlMapper xmlMapper;

    @SneakyThrows
    void sendVarsel(String aktorId) {

        NamespacedXmlVarsel xmlVarsel = lagNyttVarsel(aktorId);
        String message = xmlMapper.writeValueAsString(xmlVarsel);
        MessageCreator messageCreator = messageCreator(message, randomUUID().toString() + VARSEL_NAVN);
        varselQueue.send(messageCreator);

    }

    static NamespacedXmlVarsel lagNyttVarsel(String aktoerId) {
        return new NamespacedXmlVarsel()
                .withMottaker(new NamespacedXmlVarsel.NamespacedXmlAktoerId().withAktoerId(aktoerId))
                .withVarslingstype(new XMLVarslingstyper(VARSEL_ID, null, null));
    }

}
