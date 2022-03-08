package no.nav.fo.veilarbdialog.eskaleringsvarsel;

import lombok.RequiredArgsConstructor;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class EskaleringsvarselService {

    private final EskaleringsvarselRepository eskaleringsvarselRepository;

    private final AktorOppslagClient aktorOppslagClient;

    public void start(Fnr fnr, String begrunnelse, String overskrift, String tekst) {

    }

    public void stop(String fnr, String begrunnelse, String tekst) {

    }

    public EskaleringsvarselEntity hentGjeldende(Fnr fnr) {
        return null;
    }

    public List<EskaleringsvarselEntity> historikk(Fnr fnr) {
        return null;
    }

}
