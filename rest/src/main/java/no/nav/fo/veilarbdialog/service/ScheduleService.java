package no.nav.fo.veilarbdialog.service;

import no.nav.fo.veilarbdialog.db.dao.VarselDAO;
import no.nav.fo.veilarbdialog.util.FunksjonelleMetrikker;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLAktoerId;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarsel;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarslingstyper;
import org.slf4j.Logger;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.List;

import static java.lang.Boolean.TRUE;
import static java.util.UUID.randomUUID;
import static javax.xml.bind.JAXBContext.newInstance;
import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static javax.xml.bind.Marshaller.JAXB_FRAGMENT;
import static org.slf4j.LoggerFactory.getLogger;


@Component
public class ScheduleService {

    private static final Logger LOG = getLogger(ScheduleService.class);

    private static final long GRACE_PERIODE = Long.parseLong(System.getProperty("grace.periode.millis"));
    private static final boolean IS_MASTER = Boolean.parseBoolean(System.getProperty("cluster.ismasternode", "false"));
    private static final String VARSEL_ID = "DittNAV_000007";
    private static final String PARAGAF8_VARSEL_ID = "DittNAV_000008";
    private static final String VARSEL_NAVN = "Aktivitetsplan_nye_henvendelser";
    private static final String PARAGAF8_VARSEL_NAVN = "Aktivitetsplan_§8_mal";

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

    @Inject
    private VarselDAO varselDAO;

    @Inject
    private JmsTemplate varselQueue;

    @Inject
    private JmsTemplate stopVarselQueue;

    @Scheduled(cron = "${varslingsrate.cron}")
    public void sjekkForVarsel() {
        if (IS_MASTER) {
            List<String> varselUUIDer = varselDAO.hentRevarselrSomSkalStoppes();
            LOG.info("revarser {} som stoppes", varselUUIDer.size());
            varselUUIDer.forEach(this::stopRevarsel);
            FunksjonelleMetrikker.stopetRevarsling(varselUUIDer.size());

            List<String> aktorer = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(GRACE_PERIODE);
            LOG.info("Varsler {} brukere", aktorer.size());
            FunksjonelleMetrikker.nyeVarseler(aktorer.size());
            aktorer.forEach(this::sendVarsel);
        }
    }

    private void stopRevarsel(String varselUUID) {
        //TODO stopp revarslingen :)
        varselDAO.markerSomStoppet(varselUUID);
    }

    private void sendVarsel(String aktorId) {
        boolean paragraf8 = varselDAO.harUlesteUvarselteParagraf8Henvedelser(aktorId);
        if(paragraf8) {
            String varselUuid = sendParagraf8Varsel(aktorId);
            varselDAO.insertParagraf8Varsel(aktorId, varselUuid);
            varselDAO.setVarselUUIDForParagraf8Dialoger(aktorId, varselUuid);
        } else {
            sendServicMelding(aktorId);
        }
        varselDAO.oppdaterSisteVarselForBruker(aktorId);
    }

    private String sendParagraf8Varsel(String aktorId) {
        String uuid = randomUUID().toString() + PARAGAF8_VARSEL_NAVN;
        varselQueue.send(messageCreator(marshallVarsel(lagNyttVarsel(aktorId, PARAGAF8_VARSEL_ID)), uuid));
        return uuid;
    }

    private void sendServicMelding(String aktorId) {
        String uuid = randomUUID().toString() + VARSEL_NAVN;
        varselQueue.send(messageCreator(marshallVarsel(lagNyttVarsel(aktorId, VARSEL_ID)), uuid));
    }

    private static MessageCreator messageCreator(final String hendelse, String uuid) {
        return session -> {
            TextMessage msg = session.createTextMessage(hendelse);
            msg.setStringProperty("callId", uuid);
            return msg;
        };
    }

    private static String marshallVarsel(Object element) {
        try {
            StringWriter writer = new StringWriter();
            Marshaller marshaller = VARSEL_CONTEXT.createMarshaller();
            marshaller.setProperty(JAXB_FORMATTED_OUTPUT, TRUE);
            marshaller.setProperty(JAXB_FRAGMENT, true);
            marshaller.marshal(element, new StreamResult(writer));
            return writer.toString();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private XMLVarsel lagNyttVarsel(String aktoerId, String varselId) {
        return new XMLVarsel()
                .withMottaker(new XMLAktoerId().withAktoerId(aktoerId))
                .withVarslingstype(new XMLVarslingstyper(varselId, null, null));
    }
}
