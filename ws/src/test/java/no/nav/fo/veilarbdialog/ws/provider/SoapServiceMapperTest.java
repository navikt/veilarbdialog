package no.nav.fo.veilarbdialog.ws.provider;

import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.informasjon.Bruker;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.informasjon.Dialog;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.informasjon.Henvendelse;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.informasjon.Veileder;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;

import static no.nav.fo.veilarbdialog.domain.AvsenderType.BRUKER;
import static no.nav.fo.veilarbdialog.domain.AvsenderType.VEILEDER;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class SoapServiceMapperTest {

    private static final String PERSON_IDENT = "personIdent";
    private static final String AKTOR_ID = "aktorId";

    private SoapServiceMapper soapServiceMapper = new SoapServiceMapper();

    @Test
    public void somWSDialog_henvendelseSendtAvBruker_brukerAvsender() {
        HenvendelseData build = henvendelseBuilder().avsenderType(BRUKER).build();

        Dialog dialog = soapServiceMapper.somWSDialog(dialogMedHenvendelse(build), PERSON_IDENT);

        Henvendelse henvendelse = dialog.getHenvendelseListe().get(0);
        assertThat(henvendelse.getAvsender()).isInstanceOf(Bruker.class);
    }

    @Test
    public void somWSDialog_henvendelseSendtAvVeileder_veilederAvsender() {
        HenvendelseData henvendelseData = henvendelseBuilder().avsenderType(VEILEDER).build();

        Dialog dialog = soapServiceMapper.somWSDialog(dialogMedHenvendelse(henvendelseData), PERSON_IDENT);

        Henvendelse henvendelse = dialog.getHenvendelseListe().get(0);
        assertThat(henvendelse.getAvsender()).isInstanceOf(Veileder.class);
    }

    private DialogData dialogMedHenvendelse(HenvendelseData henvendelseData) {
        return DialogData.builder()
                .aktorId(AKTOR_ID)
                .henvendelser(Arrays.asList(henvendelseData))
                .build();
    }

    private HenvendelseData.HenvendelseDataBuilder henvendelseBuilder() {
        return HenvendelseData.builder().sendt(new Date());
    }

}