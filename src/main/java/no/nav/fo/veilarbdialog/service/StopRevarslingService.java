package no.nav.fo.veilarbdialog.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbdialog.db.dao.VarselDAO;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.ObjectFactory;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.StoppReVarsel;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;

import static no.nav.fo.veilarbdialog.util.MessageQueueUtils.*;

@Slf4j
@Component
public class StopRevarslingService {

    private static final JAXBContext STOPP_VARSEL_CONTEXT = jaxbContext(StoppReVarsel.class);

    @Inject
    private JmsTemplate stopVarselQueue;

    @Inject
    private VarselDAO varselDAO;

    void stopRevarsel(String varselUUID) {
        try {
            stopp(varselUUID);
        } catch (Exception e) {
            log.error("Feilet å sende stopp revarsel for: " + varselUUID, e);
        }
    }

    private void stopp(String varselUUID) {
        StoppReVarsel stoppReVarsel = new StoppReVarsel();
        stoppReVarsel.setVarselbestillingId(varselUUID);
        String melding = marshall(new ObjectFactory().createStoppReVarsel(stoppReVarsel), STOPP_VARSEL_CONTEXT);

        stopVarselQueue.send(messageCreator(melding, varselUUID));
        varselDAO.markerSomStoppet(varselUUID);
    }
}
