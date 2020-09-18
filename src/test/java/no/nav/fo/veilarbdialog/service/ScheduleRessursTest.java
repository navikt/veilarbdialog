package no.nav.fo.veilarbdialog.service;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.ObjectFactory;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.StoppReVarsel;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.AktoerId;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.Parameter;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.VarselMedHandling;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import javax.xml.bind.JAXBElement;

import static org.assertj.core.api.Assertions.assertThat;

public class ScheduleRessursTest {

    private final XmlMapper xmlMapper = new XmlMapper();
    
    @Test
    public void skalMarshalleStoppVarselMeldingUtenException()
            throws Exception {
        StoppReVarsel stoppReVarsel = new StoppReVarsel();
        stoppReVarsel.setVarselbestillingId("UUID-123");
        String melding = xmlMapper.writeValueAsString(new ObjectFactory().createStoppReVarsel(stoppReVarsel).getValue());
        assertThat(melding).contains("UUID-123");
    }

    @Test
    public void skalMarshalleVarselMeldingUtenException()
            throws Exception {
        String melding = xmlMapper.writeValueAsString(ServiceMeldingService.lagNyttVarsel("aktoer-123"));
        assertThat(melding).contains("aktoer-123");
        assertThat(melding).contains("DittNAV_000007");
    }

}
