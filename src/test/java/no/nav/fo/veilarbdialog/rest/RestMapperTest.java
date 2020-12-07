package no.nav.fo.veilarbdialog.rest;

import no.nav.common.abac.Pep;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import org.junit.Test;

import java.util.Date;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class RestMapperTest {

    private final Pep pep = mock(Pep.class);
    private final RestMapper restMapper = new RestMapper(new AuthService(pep));

    @Test
    public void skal_inneholde_alle_henvendelser_dersom_ikke_kontorsperret() {
        when(pep.harVeilederTilgangTilEnhet(any(), anyString()))
                .thenReturn(true);
        DialogDTO dialogDto = restMapper.somDialogDTO(nyDialog(nyHenvendelse(1, null)));

        assertThat(dialogDto.henvendelser.size()).isEqualTo(1);
    }

    private DialogData nyDialog(HenvendelseData... henvendelser) {
        return DialogData.builder().id(1).henvendelser(asList(henvendelser)).build();
    }

    private HenvendelseData nyHenvendelse(int id, String kontorsperreEnhetId) {
        return HenvendelseData.builder().id(id).sendt(new Date()).kontorsperreEnhetId(kontorsperreEnhetId).build();
    }

    @Test
    public void skal_inneholde_kontorsperrede_henvendelser_dersom_tilgang() {
        String kontorsperretEnhet = "123";
        when(pep.harVeilederTilgangTilEnhet(any(), anyString()))
                .thenReturn(false);
        when(pep.harVeilederTilgangTilEnhet(any(), eq(kontorsperretEnhet)))
                .thenReturn(true);

        DialogDTO dialogDto = restMapper.somDialogDTO(nyDialog(nyHenvendelse(1, null), nyHenvendelse(2, kontorsperretEnhet), nyHenvendelse(3, "")));
        assertThat(dialogDto.henvendelser.size()).isEqualTo(3);
        assertThat(dialogDto.henvendelser.stream().noneMatch(h -> ("2".equals(h.id)))).isFalse();
    }

    @Test
    public void skal_fjerne_kontorsperrede_henvendelser_dersom_ikke_tilgang() {
        String kontorsperretEnhet = "123";
        when(pep.harVeilederTilgangTilEnhet(any(), anyString()))
                .thenReturn(true);
        when(pep.harVeilederTilgangTilEnhet(any(), eq(kontorsperretEnhet)))
                .thenReturn(false);

        DialogDTO dialogDto = restMapper.somDialogDTO(nyDialog(nyHenvendelse(1, null), nyHenvendelse(2, kontorsperretEnhet), nyHenvendelse(3, "")));
        assertThat(dialogDto.henvendelser.size()).isEqualTo(2);
        assertThat(dialogDto.henvendelser.stream().noneMatch(h -> ("2".equals(h.id)))).isTrue();
    }
}
