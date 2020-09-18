package no.nav.fo.veilarbdialog.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.AktoerId;
import no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.OppgaveType;
import no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.Oppgavehenvendelse;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import static no.nav.fo.veilarbdialog.util.MessageQueueUtils.messageCreator;

@Service
@RequiredArgsConstructor
public class OppgaveService {

    private final ServiceConfig config;
    private final JmsTemplate oppgaveHenvendelseQueue;
    private final XmlMapper xmlMapper;

    @SneakyThrows
    public void send(String aktorId, String varselId) {
        String xml = xmlMapper.writeValueAsString(createMelding(aktorId, varselId));
        oppgaveHenvendelseQueue.send(messageCreator(xml, varselId));
    }

    Oppgavehenvendelse createMelding(String aktorid, String uuid) {
        AktoerId aktoerId = new AktoerId();
        aktoerId.setAktoerId(aktorid);

        OppgaveType oppgaveType = new OppgaveType();
        oppgaveType.setValue("0004");

        Oppgavehenvendelse henvendelse = new Oppgavehenvendelse();
        henvendelse.setMottaker(aktoerId);
        henvendelse.setOppgaveType(oppgaveType);
        henvendelse.setVarselbestillingId(uuid);
        henvendelse.setOppgaveURL(config.getArbeidsrettetDialogUrl().toString());
        henvendelse.setStoppRepeterendeVarsel(false);

        return henvendelse;
    }
}
