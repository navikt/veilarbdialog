package no.nav.fo.veilarbdialog.jms;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLAktoer;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLAktoerId;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarsel;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarslingstyper;

@JacksonXmlRootElement(localName = "ns2:Varsel")
public class NamespacedXmlVarsel extends XMLVarsel {

    @JacksonXmlProperty(localName = "xmlns:ns2", isAttribute = true)
    protected String xmlns = "http://nav.no/melding/virksomhet/varsel/v1/varsel";

    @Override
    public NamespacedXmlVarsel withMottaker(XMLAktoer value) {
        super.setMottaker(value);
        return this;
    }

    @Override
    public NamespacedXmlVarsel withVarslingstype(XMLVarslingstyper value) {
        super.withVarslingstype(value);
        return this;
    }

    public static class NamespacedXmlAktoerId extends XMLAktoerId {

        @JacksonXmlProperty(localName = "xsi:type", isAttribute = true)
        protected String type = "ns2:AktoerId";

        @JacksonXmlProperty(localName = "xmlns:xsi", isAttribute = true)
        protected String xmlns = "http://www.w3.org/2001/XMLSchema-instance";

        @Override
        public NamespacedXmlAktoerId withAktoerId(String value) {
            super.withAktoerId(value);
            return this;
        }

    }

}