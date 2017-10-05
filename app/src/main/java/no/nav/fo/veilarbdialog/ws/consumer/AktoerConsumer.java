package no.nav.fo.veilarbdialog.ws.consumer;

import no.nav.dialogarena.aktor.AktorService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class AktoerConsumer {

    private static final Logger LOG = getLogger(AktoerConsumer.class);

//    @Inject
//    private Aktoer_v2PortType aktoerV2;

    @Inject
    private AktorService aktorService;

    public Optional<String> hentAktoerIdForIdent(String ident) {
//        if (isBlank(ident)) {
//            LOG.warn("Kan ikke hente aktør-id uten fødselsnummer");
//            return empty();
//        }
//        try {
//            return of(aktoerV2.hentAktoerIdForIdent(
//                    new WSHentAktoerIdForIdentRequest()
//                            .withIdent(ident)
//            ).getAktoerId());
//        } catch (HentAktoerIdForIdentPersonIkkeFunnet e) {
//            LOG.warn("AktoerID ikke funnet for fødselsnummer!", e);
//            return empty();
//        }

        return Optional.ofNullable(aktorService.getAktorId(ident));
    }

    public Optional<String> hentIdentForAktorId(String aktorId) {
//        if (isBlank(aktorId)) {
//            LOG.warn("Kan ikke hente fødselsnummer uten aktør-id");
//            return empty();
//        }
//        try {
//            return of(aktoerV2.hentIdentForAktoerId(
//                    new WSHentIdentForAktoerIdRequest()
//                            .withAktoerId(aktorId)
//            ).getIdent());
//        } catch (HentIdentForAktoerIdPersonIkkeFunnet e) {
//            LOG.warn("fødselsnummer ikke funnet for aktoerID!", e);
//            return empty();
//        }
        return Optional.ofNullable(aktorService.getFnr(aktorId));
    }

}
