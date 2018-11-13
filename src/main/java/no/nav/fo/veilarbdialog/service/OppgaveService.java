package no.nav.fo.veilarbdialog.service;

import no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.AktoerId;
import no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.ObjectFactory;
import no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.OppgaveType;
import no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.Oppgavehenvendelse;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import static javax.xml.bind.JAXBContext.newInstance;
import static no.nav.fo.veilarbdialog.ApplicationContext.AKTIVITETSPLAN_URL_PROPERTY;
import static no.nav.fo.veilarbdialog.service.Utils.marshall;
import static no.nav.fo.veilarbdialog.service.Utils.messageCreator;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Component
public class OppgaveService {

    private String aktivitetsplanBaseUrl = getRequiredProperty(AKTIVITETSPLAN_URL_PROPERTY);


    @Inject
    private JmsTemplate oppgaveHenvendelseQueue;

    private static final JAXBContext OPPGAVE_HENVENDELSE;

    static {
        try {
            OPPGAVE_HENVENDELSE = newInstance(
                    Oppgavehenvendelse.class
            );
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(String aktorId, String varselId) {
        MessageCreator messageCreator = messageCreator(marshall(createMelding(aktorId, varselId), OPPGAVE_HENVENDELSE), varselId);
        oppgaveHenvendelseQueue.send(messageCreator);
    }

    JAXBElement<Oppgavehenvendelse> createMelding(String aktorid, String uuid) {
        AktoerId aktoerId = new AktoerId();
        aktoerId.setAktoerId(aktorid);

        OppgaveType oppgaveType = new OppgaveType();
        oppgaveType.setValue("0004");

        Oppgavehenvendelse henvendelse = new Oppgavehenvendelse();
        henvendelse.setMottaker(aktoerId);
        henvendelse.setOppgaveType(oppgaveType);
        henvendelse.setVarselbestillingId(uuid);
        henvendelse.setOppgaveURL(aktivitetsplanBaseUrl + "/dialog");
        henvendelse.setStoppRepeterendeVarsel(false);

        return new ObjectFactory().createOppgavehenvendelse(henvendelse);
    }
}
