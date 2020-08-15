package no.nav.fo.veilarbdialog.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.fo.veilarbdialog.jms.NamespacedVarselMedHandling;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.Parameter;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import static no.nav.fo.veilarbdialog.util.MessageQueueUtils.messageCreator;

@Service
@RequiredArgsConstructor
public class VarselMedHandlingService {

    private static final String PARAGAF8_VARSEL_ID = "DittNAV_000008";

    private final JmsTemplate varselMedHandlingQueue;
    private final XmlMapper xmlMapper;

    @SneakyThrows
    public void send(String aktorId, String varselbestillingId) {

        NamespacedVarselMedHandling.NamespacedAktoerId mottaker = new NamespacedVarselMedHandling.NamespacedAktoerId();
        mottaker.setAktoerId(aktorId);

        NamespacedVarselMedHandling varselMedHandling = new NamespacedVarselMedHandling();
        varselMedHandling.setVarseltypeId(PARAGAF8_VARSEL_ID);
        varselMedHandling.setReVarsel(false);
        varselMedHandling.setMottaker(mottaker);
        varselMedHandling.setVarselbestillingId(varselbestillingId);

        Parameter parameter = new Parameter();
        parameter.setKey("varselbestillingId");
        parameter.setValue(varselbestillingId);
        varselMedHandling
                .getParameterListe()
                .add(parameter);

        varselMedHandlingQueue.send(messageCreator(xmlMapper.writeValueAsString(varselMedHandling), varselbestillingId));

    }

}
