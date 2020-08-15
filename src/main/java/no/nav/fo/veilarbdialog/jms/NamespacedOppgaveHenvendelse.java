package no.nav.fo.veilarbdialog.jms;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.AktoerId;
import no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.Oppgavehenvendelse;

@JacksonXmlRootElement(localName = "Oppgavehenvendelse")
public class NamespacedOppgaveHenvendelse extends Oppgavehenvendelse {

    @JacksonXmlProperty(isAttribute = true)
    protected String xmlns = "http://nav.no/melding/virksomhet/opprettOppgavehenvendelse/v1/opprettOppgavehenvendelse";

    public static class NamespacedAktoerId extends AktoerId {

        @JacksonXmlProperty(isAttribute = true)
        protected String type = "AktoerId";

        @JacksonXmlProperty(isAttribute = true)
        protected String xmlns = "http://www.w3.org/2001/XMLSchema-instance";

    }

}
