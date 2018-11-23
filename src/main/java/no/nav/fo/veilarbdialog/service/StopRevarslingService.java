package no.nav.fo.veilarbdialog.service;

import no.nav.fo.veilarbdialog.db.dao.VarselDAO;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.ObjectFactory;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.StoppReVarsel;
import org.slf4j.Logger;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import static javax.xml.bind.JAXBContext.newInstance;
import static no.nav.fo.veilarbdialog.service.Utils.marshall;
import static no.nav.fo.veilarbdialog.service.Utils.messageCreator;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class StopRevarslingService {

    static final JAXBContext STOPP_VARSEL_CONTEXT;

    private static final Logger LOG = getLogger(StopRevarslingService.class);


    static {
        try {
            STOPP_VARSEL_CONTEXT = newInstance(
                    StoppReVarsel.class
            );
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    @Inject
    private JmsTemplate stopVarselQueue;
    @Inject
    private VarselDAO varselDAO;

    public void stopRevarsel(String varselUUID) {
        try {
            stopp(varselUUID);
        } catch (Exception e) {
            LOG.error("feilet med å sende stopp revarsel for: " + varselUUID, e );
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
