package no.nav.fo.veilarbdialog.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.AktoerId;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.ObjectFactory;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.Parameter;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.VarselMedHandling;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;

import static no.nav.fo.veilarbdialog.util.MessageQueueUtils.messageCreator;

@Component
@RequiredArgsConstructor
public class VarselMedHandlingService {

    private static final String PARAGAF8_VARSEL_ID = "DittNAV_000008";

    private final JmsTemplate varselMedHandlingQueue;
    private final XmlMapper xmlMapper;

    @SneakyThrows
    public void send(String aktorId, String varselbestillingId) {
        AktoerId motaker = new AktoerId();
        motaker.setAktoerId(aktorId);
        VarselMedHandling varselMedHandling = new VarselMedHandling();
        varselMedHandling.setVarseltypeId(PARAGAF8_VARSEL_ID);
        varselMedHandling.setReVarsel(false);
        varselMedHandling.setMottaker(motaker);
        varselMedHandling.setVarselbestillingId(varselbestillingId);

        Parameter parameter = new Parameter();
        parameter.setKey("varselbestillingId");
        parameter.setValue(varselbestillingId);

        varselMedHandling
                .getParameterListe()
                .add(parameter);

        JAXBElement<VarselMedHandling> melding = new ObjectFactory().createVarselMedHandling(varselMedHandling);

        varselMedHandlingQueue.send(messageCreator(xmlMapper.writeValueAsString(melding.getValue()), varselbestillingId));
    }

}
