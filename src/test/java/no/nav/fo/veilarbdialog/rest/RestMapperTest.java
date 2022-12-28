
package no.nav.fo.veilarbdialog.rest;

import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;
import no.nav.fo.veilarbdialog.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RestMapperTest {

    private RestMapper restMapper;
    private KontorsperreFilter filter;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        filter = mock(KontorsperreFilter.class);
        auth = mock(AuthService.class);
        restMapper = new RestMapper(filter, auth);
    }

    @Test
    void somDialogDTO_henvendelseUtenKontorsperre_skalReturnereHenvendelse() {
        when(filter.tilgangTilEnhet(isA(HenvendelseData.class))).thenReturn(true);
        DialogDTO dialogDto = restMapper.somDialogDTO(nyDialog(nyHenvendelse(1, null)));

        assertThat(dialogDto.getHenvendelser()).hasSize(1);
    }

    private DialogData nyDialog(HenvendelseData... henvendelser) {
        return DialogData.builder().id(1).henvendelser(asList(henvendelser)).build();
    }

    private HenvendelseData nyHenvendelse(int id, String kontorsperreEnhetId) {
        return HenvendelseData.builder().id(id).sendt(new Date()).kontorsperreEnhetId(kontorsperreEnhetId).build();
    }

    @Test
    void somDialogDTO_manglerTilgang_skalIkkeReturnereHenvendelse() {
        when(filter.tilgangTilEnhet(isA(HenvendelseData.class))).thenReturn(false);

        DialogDTO dialogDto = restMapper.somDialogDTO(nyDialog(nyHenvendelse(1, null)));

        assertThat(dialogDto.getHenvendelser()).isEmpty();
    }
}

