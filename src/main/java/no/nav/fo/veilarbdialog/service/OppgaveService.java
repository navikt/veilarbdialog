package no.nav.fo.veilarbdialog.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.AktoerId;
import no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.ObjectFactory;
import no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.OppgaveType;
import no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.Oppgavehenvendelse;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;

import static no.nav.fo.veilarbdialog.util.MessageQueueUtils.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OppgaveService {

    private static final JAXBContext OPPGAVE_HENVENDELSE = jaxbContext(Oppgavehenvendelse.class);

    private final ServiceConfig config;
    private final JmsTemplate oppgaveHenvendelseQueue;

    @SneakyThrows
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
        henvendelse.setOppgaveURL(config.getArbeidsrettetDialogUrl());
        henvendelse.setStoppRepeterendeVarsel(false);

        return new ObjectFactory().createOppgavehenvendelse(henvendelse);
    }
}
