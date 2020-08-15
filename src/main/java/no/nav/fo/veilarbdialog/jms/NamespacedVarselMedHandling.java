package no.nav.fo.veilarbdialog.jms;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.Aktoer;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.AktoerId;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.VarselMedHandling;

@JacksonXmlRootElement(localName = "VarselMedHandling")
public class NamespacedVarselMedHandling extends VarselMedHandling {

    @JacksonXmlProperty(isAttribute = true)
    protected String xmlns = "http://nav.no/melding/virksomhet/varselMedHandling/v1/varselMedHandling";

    public static class NamespacedAktoerId extends AktoerId {

        @JacksonXmlProperty(isAttribute = true)
        protected String type = "AktoerId";

        @JacksonXmlProperty(isAttribute = true)
        protected String xmlns = "http://www.w3.org/2001/XMLSchema-instance";

    }

}
