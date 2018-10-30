package no.nav.fo.veilarbdialog.service;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.*;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import static javax.xml.bind.JAXBContext.newInstance;
import static no.nav.fo.veilarbdialog.service.Utils.marshall;
import static no.nav.fo.veilarbdialog.service.Utils.messageCreator;

@Component
public class VarselMedHandlingService {

    private static final String PARAGAF8_VARSEL_ID = "DittNAV_000008";
    public static final String PARAGAF8_VARSEL_NAVN = "Aktivitetsplan_p8_mal";

    @Inject
    private JmsTemplate varselMedHandlingQueue;

    private static final JAXBContext VARSEL_MED_HANDLING;

    static {
        try {
            VARSEL_MED_HANDLING = newInstance(
                    ObjectFactory.class
            );
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(String aktorId, String varselbestillingId) {
        AktoerId motaker = new AktoerId();
        motaker.setAktoerId(aktorId);
        VarselMedHandling varselMedHandling = new VarselMedHandling();
        varselMedHandling.setVarseltypeId(PARAGAF8_VARSEL_ID);
        varselMedHandling.setReVarsel(false);
        varselMedHandling.setMottaker(motaker);
        varselMedHandling.setVarselbestillingId(varselbestillingId);

        JAXBElement<VarselMedHandling> melding = new ObjectFactory().createVarselMedHandling(varselMedHandling);

        varselMedHandlingQueue.send(messageCreator(marshall(melding, VARSEL_MED_HANDLING), varselbestillingId));
    }
}
