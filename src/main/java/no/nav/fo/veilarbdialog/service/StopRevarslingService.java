package no.nav.fo.veilarbdialog.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbdialog.db.dao.VarselDAO;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.ObjectFactory;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.StoppReVarsel;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import static no.nav.fo.veilarbdialog.util.MessageQueueUtils.messageCreator;

@Component
@RequiredArgsConstructor
@Slf4j
public class StopRevarslingService {

    private final JmsTemplate stopVarselQueue;
    private final VarselDAO varselDAO;
    private final XmlMapper xmlMapper;

    void stopRevarsel(String varselUUID) {
        try {
            stopp(varselUUID);
        } catch (Exception e) {
            log.error("Feilet å sende stopp revarsel for: " + varselUUID, e);
        }
    }

    @SneakyThrows
    private void stopp(String varselUUID) {
        StoppReVarsel stoppReVarsel = new StoppReVarsel();
        stoppReVarsel.setVarselbestillingId(varselUUID);
        String melding = xmlMapper.writeValueAsString(new ObjectFactory().createStoppReVarsel(stoppReVarsel));

        stopVarselQueue.send(messageCreator(melding, varselUUID));
        varselDAO.markerSomStoppet(varselUUID);
    }
}
