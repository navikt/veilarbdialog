package no.nav.fo.veilarbdialog.service;

import no.nav.fo.veilarbdialog.db.dao.VarselDAO;
import no.nav.fo.veilarbdialog.util.FunksjonelleMetrikker;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.ObjectFactory;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.StoppReVarsel;
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

    private static final long GRACE_PERIODE = Long.parseLong(System.getProperty("grace.periode.millis", String.valueOf(24*60*60*1000)));
    private static final boolean IS_MASTER = Boolean.parseBoolean(System.getProperty("cluster.ismasternode", "false"));
    private static final String VARSEL_ID = "DittNAV_000007";
    private static final String PARAGAF8_VARSEL_ID = "DittNAV_000008";
    private static final String VARSEL_NAVN = "Aktivitetsplan_nye_henvendelser";
    private static final String PARAGAF8_VARSEL_NAVN = "Aktivitetsplan_§8_mal";

    static final JAXBContext VARSEL_CONTEXT;
    static final JAXBContext STOPP_VARSEL_CONTEXT;

    static {
        try {
            VARSEL_CONTEXT = newInstance(
                    XMLVarsel.class,
                    XMLVarslingstyper.class
            );
            STOPP_VARSEL_CONTEXT = newInstance(
                  StoppReVarsel.class
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
            List<String> varselUUIDer = varselDAO.hentRevarslerSomSkalStoppes();
            LOG.info("Stopper {} revarsler", varselUUIDer.size());
            varselUUIDer.forEach(this::stopRevarsel);
            FunksjonelleMetrikker.stoppetRevarsling(varselUUIDer.size());

            List<String> aktorer = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(GRACE_PERIODE);
            LOG.info("Varsler {} brukere", aktorer.size());
            FunksjonelleMetrikker.nyeVarsler(aktorer.size());
            aktorer.forEach(this::sendVarsel);
        }
    }

    private void stopRevarsel(String varselUUID) {
        StoppReVarsel stoppReVarsel = new StoppReVarsel();
        stoppReVarsel.setVarselbestillingId(varselUUID);
        String melding = marshall(new ObjectFactory().createStoppReVarsel(stoppReVarsel), STOPP_VARSEL_CONTEXT);
        stopVarselQueue.send(messageCreator(melding, varselUUID));
        varselDAO.markerSomStoppet(varselUUID);
    }

    private void sendVarsel(String aktorId) {
        boolean paragraf8 = varselDAO.harUlesteUvarsledeParagraf8Henvendelser(aktorId);
        if (paragraf8) {
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
        varselQueue.send(messageCreator(marshall(lagNyttVarsel(aktorId, PARAGAF8_VARSEL_ID), VARSEL_CONTEXT), uuid));
        FunksjonelleMetrikker.paragraf8Varsel();
        return uuid;
    }

    private void sendServicMelding(String aktorId) {
        String uuid = randomUUID().toString() + VARSEL_NAVN;
        varselQueue.send(messageCreator(marshall(lagNyttVarsel(aktorId, VARSEL_ID), VARSEL_CONTEXT), uuid));
    }

    private static MessageCreator messageCreator(final String hendelse, String uuid) {
        return session -> {
            TextMessage msg = session.createTextMessage(hendelse);
            msg.setStringProperty("callId", uuid);
            return msg;
        };
    }

    static String marshall(Object element, JAXBContext jaxbContext) {
        try {
            StringWriter writer = new StringWriter();
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(JAXB_FORMATTED_OUTPUT, TRUE);
            marshaller.setProperty(JAXB_FRAGMENT, true);
            marshaller.marshal(element, new StreamResult(writer));
            return writer.toString();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    static XMLVarsel lagNyttVarsel(String aktoerId, String varselId) {
        return new XMLVarsel()
                .withMottaker(new XMLAktoerId().withAktoerId(aktoerId))
                .withVarslingstype(new XMLVarslingstyper(varselId, null, null));
    }


}
