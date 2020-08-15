package no.nav.fo.veilarbdialog.jms;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.StoppReVarsel;

@JacksonXmlRootElement(localName = "StoppReVarsel")
public class NamespacedStoppReVarsel extends StoppReVarsel {

    @JacksonXmlProperty(isAttribute = true)
    protected String xmlns = "http://nav.no/melding/virksomhet/stoppReVarsel/v1/stoppReVarsel";



}
