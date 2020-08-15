package no.nav.fo.veilarbdialog.util;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import no.nav.fo.veilarbdialog.config.ApplicationConfig;
import no.nav.fo.veilarbdialog.jms.NamespacedOppgaveHenvendelse;
import no.nav.fo.veilarbdialog.jms.NamespacedStoppReVarsel;
import no.nav.fo.veilarbdialog.jms.NamespacedVarselMedHandling;
import no.nav.fo.veilarbdialog.jms.NamespacedXmlVarsel;
import no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.OppgaveType;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarslingstyper;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.Parameter;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.VarselMedHandling;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TODO: Split these tests into tests belonging to the actual usage in code (for example marshallingOfXMLVarsel(...) belongs in ServiceMeldingServiceTest).
 * <p>
 * Kept here for now for easier comparison with old code.
 */
public class MessageQueueUtilsTest {

    private final XmlMapper xmlMapper = new ApplicationConfig().xmlMapper();

    @Test
    public void marshallingOfXMLVarsel()
            throws Exception {

        String expected = "<ns2:Varsel xmlns:ns2=\"http://nav.no/melding/virksomhet/varsel/v1/varsel\">\n" +
                "    <mottaker xsi:type=\"ns2:AktoerId\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "        <aktoerId>123</aktoerId>\n" +
                "    </mottaker>\n" +
                "    <varslingstype>DittNAV_000007</varslingstype>\n" +
                "</ns2:Varsel>";

        String aktorId = "123";
        String varselId = "DittNAV_000007";


        NamespacedXmlVarsel xmlVarsel = new NamespacedXmlVarsel()
                .withMottaker(new NamespacedXmlVarsel.NamespacedXmlAktoerId().withAktoerId(aktorId))
                .withVarslingstype(new XMLVarslingstyper(varselId, null, null));
        String marshalled = xmlMapper.writeValueAsString(xmlVarsel);
        System.out.println(marshalled);

        assertThat(marshalled).isEqualToIgnoringWhitespace(expected);

    }

    @Test
    public void marshallingOfStopReVarsel()
            throws Exception {

        String expected = "<StoppReVarsel xmlns=\"http://nav.no/melding/virksomhet/stoppReVarsel/v1/stoppReVarsel\">\n" +
                "    <varselbestillingId>some_fake_uuid</varselbestillingId>\n" +
                "</StoppReVarsel>";

        String varselUUID = "some_fake_uuid";

        NamespacedStoppReVarsel stoppReVarsel = new NamespacedStoppReVarsel();
        stoppReVarsel.setVarselbestillingId(varselUUID);
        String marshalled = xmlMapper.writeValueAsString(stoppReVarsel);

        assertThat(marshalled).isEqualToIgnoringWhitespace(expected);

    }

    @Test
    public void marshallingOfVarselMedHandling()
            throws Exception {

        String expected = "<VarselMedHandling xmlns=\"http://nav.no/melding/virksomhet/varselMedHandling/v1/varselMedHandling\">\n" +
                "    <mottaker type=\"AktoerId\" xmlns=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "        <aktoerId>123</aktoerId>\n" +
                "    </mottaker>\n" +
                "    <varselbestillingId>456</varselbestillingId>\n" +
                "    <reVarsel>false</reVarsel>\n" +
                "    <varseltypeId>DittNAV_000008</varseltypeId>\n" +
                "    <parameterListe>\n" +
                "        <parameterListe>\n" +
                "            <key>varselbestillingId</key>\n" +
                "            <value>456</value>\n" +
                "        </parameterListe>\n" +
                "    </parameterListe>\n" +
                "</VarselMedHandling>";

        String aktorId = "123";
        String paragaf8VarselId = "DittNAV_000008";
        String varselbestillingId = "456";

        NamespacedVarselMedHandling.NamespacedAktoerId mottaker = new NamespacedVarselMedHandling.NamespacedAktoerId();
        mottaker.setAktoerId(aktorId);
        VarselMedHandling varselMedHandling = new NamespacedVarselMedHandling();
        varselMedHandling.setVarseltypeId(paragaf8VarselId);
        varselMedHandling.setReVarsel(false);
        varselMedHandling.setMottaker(mottaker);
        varselMedHandling.setVarselbestillingId(varselbestillingId);
        Parameter parameter = new Parameter();
        parameter.setKey("varselbestillingId");
        parameter.setValue(varselbestillingId);
        varselMedHandling
                .getParameterListe()
                .add(parameter);
        String marshalled = xmlMapper.writeValueAsString(varselMedHandling);

        assertThat(marshalled).isEqualToIgnoringWhitespace(expected);

    }

    @Test
    public void marshallingOfOppgavehenvendelse()
            throws Exception {

        String expected = "<Oppgavehenvendelse xmlns=\"http://nav.no/melding/virksomhet/opprettOppgavehenvendelse/v1/opprettOppgavehenvendelse\">\n" +
                "    <mottaker type=\"AktoerId\" xmlns=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "        <aktoerId>123</aktoerId>\n" +
                "    </mottaker>\n" +
                "    <oppgaveType>0004</oppgaveType>\n" +
                "    <oppgaveURL>http://some.fake.url</oppgaveURL>\n" +
                "    <varselbestillingId>some_fake_uuid</varselbestillingId>\n" +
                "    <stoppRepeterendeVarsel>false</stoppRepeterendeVarsel>\n" +
                "</Oppgavehenvendelse>";

        String aktorId = "123";
        String uuid = "some_fake_uuid";
        String dialogUrl = "http://some.fake.url";

        NamespacedOppgaveHenvendelse.NamespacedAktoerId aktoerId = new NamespacedOppgaveHenvendelse.NamespacedAktoerId();
        aktoerId.setAktoerId(aktorId);
        OppgaveType oppgaveType = new OppgaveType();
        oppgaveType.setValue("0004");
        NamespacedOppgaveHenvendelse henvendelse = new NamespacedOppgaveHenvendelse();
        henvendelse.setMottaker(aktoerId);
        henvendelse.setOppgaveType(oppgaveType);
        henvendelse.setVarselbestillingId(uuid);
        henvendelse.setOppgaveURL(dialogUrl);
        henvendelse.setStoppRepeterendeVarsel(false);
        String marshalled = xmlMapper.writeValueAsString(henvendelse);

        assertThat(marshalled).isEqualToIgnoringWhitespace(expected);

    }

}