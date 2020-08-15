package no.nav.fo.veilarbdialog.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.StoppReVarsel;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ScheduleRessursTest {

    private final XmlMapper xmlMapper = new XmlMapper();

    @Test
    public void skalMarshalleStoppVarselMeldingUtenException()
            throws Exception {
        StoppReVarsel stoppReVarsel = new StoppReVarsel();
        stoppReVarsel.setVarselbestillingId("UUID-123");
        String melding = xmlMapper.writeValueAsString(stoppReVarsel);
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
