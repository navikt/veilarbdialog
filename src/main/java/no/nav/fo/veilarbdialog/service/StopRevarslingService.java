package no.nav.fo.veilarbdialog.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbdialog.db.dao.VarselDAO;
import no.nav.fo.veilarbdialog.jms.NamespacedStoppReVarsel;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import static no.nav.fo.veilarbdialog.util.MessageQueueUtils.messageCreator;

@Service
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

        NamespacedStoppReVarsel stoppReVarsel = new NamespacedStoppReVarsel();
        stoppReVarsel.setVarselbestillingId(varselUUID);
        String melding = xmlMapper.writeValueAsString(stoppReVarsel);
        stopVarselQueue.send(messageCreator(melding, varselUUID));
        varselDAO.markerSomStoppet(varselUUID);

    }
}
