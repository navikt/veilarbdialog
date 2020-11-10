package no.nav.fo.veilarbdialog.util;

import no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.OppgaveType;
import no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.Oppgavehenvendelse;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.ObjectFactory;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.StoppReVarsel;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLAktoerId;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarsel;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarslingstyper;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.AktoerId;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.Parameter;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.VarselMedHandling;
import org.junit.Test;
import org.springframework.jms.core.MessageCreator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;

import java.util.UUID;

import static no.nav.fo.veilarbdialog.util.MessageQueueUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

public class MessageQueueUtilsTest {

    @Test
    public void marshallingOfXMLVarsel() {

        String expected = "<ns2:Varsel xmlns:ns2=\"http://nav.no/melding/virksomhet/varsel/v1/varsel\">\n" +
                "    <mottaker xsi:type=\"ns2:AktoerId\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "        <aktoerId>123</aktoerId>\n" +
                "    </mottaker>\n" +
                "    <varslingstype>DittNAV_000007</varslingstype>\n" +
                "</ns2:Varsel>";

        String aktorId = "123";
        String varselId = "DittNAV_000007";
        JAXBContext context = jaxbContext(XMLVarsel.class, XMLVarslingstyper.class);

        XMLVarsel xmlVarsel = new XMLVarsel()
                .withMottaker(new XMLAktoerId().withAktoerId(aktorId))
                .withVarslingstype(new XMLVarslingstyper(varselId, null, null));
        String marshalled = marshall(xmlVarsel, context);

        assertThat(marshalled).isEqualTo(expected);

    }

    @Test
    public void marshallingOfStopReVarsel() {

        String expected = "<ns2:stoppReVarsel xmlns:ns2=\"http://nav.no/melding/virksomhet/stoppReVarsel/v1/stoppReVarsel\">\n" +
                "    <varselbestillingId>some_fake_uuid</varselbestillingId>\n" +
                "</ns2:stoppReVarsel>";

        String varselUUID = "some_fake_uuid";
        JAXBContext context = jaxbContext(StoppReVarsel.class);

        StoppReVarsel stoppReVarsel = new StoppReVarsel();
        stoppReVarsel.setVarselbestillingId(varselUUID);
        String marshalled = marshall(new ObjectFactory().createStoppReVarsel(stoppReVarsel), context);

        assertThat(marshalled).isEqualTo(expected);

    }

    @Test
    public void marshallingOfVarselMedHandling() {

        String expected = "<ns2:varselMedHandling xmlns:ns2=\"http://nav.no/melding/virksomhet/varselMedHandling/v1/varselMedHandling\">\n" +
                "    <mottaker xsi:type=\"ns2:AktoerId\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "        <aktoerId>123</aktoerId>\n" +
                "    </mottaker>\n" +
                "    <varselbestillingId>456</varselbestillingId>\n" +
                "    <reVarsel>false</reVarsel>\n" +
                "    <varseltypeId>DittNAV_000008</varseltypeId>\n" +
                "    <parameterListe>\n" +
                "        <key>varselbestillingId</key>\n" +
                "        <value>456</value>\n" +
                "    </parameterListe>\n" +
                "</ns2:varselMedHandling>";

        String aktorId = "123";
        String paragaf8VarselId = "DittNAV_000008";
        String varselbestillingId = "456";
        JAXBContext context = jaxbContext(no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.ObjectFactory.class);

        AktoerId motaker = new AktoerId();
        motaker.setAktoerId(aktorId);
        VarselMedHandling varselMedHandling = new VarselMedHandling();
        varselMedHandling.setVarseltypeId(paragaf8VarselId);
        varselMedHandling.setReVarsel(false);
        varselMedHandling.setMottaker(motaker);
        varselMedHandling.setVarselbestillingId(varselbestillingId);
        Parameter parameter = new Parameter();
        parameter.setKey("varselbestillingId");
        parameter.setValue(varselbestillingId);
        varselMedHandling
                .getParameterListe()
                .add(parameter);
        JAXBElement<VarselMedHandling> melding = new no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.ObjectFactory().createVarselMedHandling(varselMedHandling);
        String marshalled = marshall(melding, context);

        assertThat(marshalled).isEqualTo(expected);

    }

    @Test
    public void marshallingOfOppgavehenvendelse() {

        String expected = "<ns2:oppgavehenvendelse xmlns:ns2=\"http://nav.no/melding/virksomhet/opprettOppgavehenvendelse/v1/opprettOppgavehenvendelse\">\n" +
                "    <mottaker xsi:type=\"ns2:AktoerId\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "        <aktoerId>123</aktoerId>\n" +
                "    </mottaker>\n" +
                "    <oppgaveType>0004</oppgaveType>\n" +
                "    <oppgaveURL>http://some.fake.url</oppgaveURL>\n" +
                "    <varselbestillingId>some_fake_uuid</varselbestillingId>\n" +
                "    <stoppRepeterendeVarsel>false</stoppRepeterendeVarsel>\n" +
                "</ns2:oppgavehenvendelse>";

        String aktorId = "123";
        String uuid = "some_fake_uuid";
        String dialogUrl = "http://some.fake.url";
        JAXBContext context = jaxbContext(Oppgavehenvendelse.class);

        no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.AktoerId aktoerId = new no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.AktoerId();
        aktoerId.setAktoerId(aktorId);
        OppgaveType oppgaveType = new OppgaveType();
        oppgaveType.setValue("0004");
        Oppgavehenvendelse henvendelse = new Oppgavehenvendelse();
        henvendelse.setMottaker(aktoerId);
        henvendelse.setOppgaveType(oppgaveType);
        henvendelse.setVarselbestillingId(uuid);
        henvendelse.setOppgaveURL(dialogUrl);
        henvendelse.setStoppRepeterendeVarsel(false);
        JAXBElement<Oppgavehenvendelse> oppgavehenvendelse = new no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.ObjectFactory().createOppgavehenvendelse(henvendelse);
        String marshalled = marshall(oppgavehenvendelse, context);

        assertThat(marshalled).isEqualTo(expected);

    }

}