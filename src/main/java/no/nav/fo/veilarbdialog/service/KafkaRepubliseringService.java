package no.nav.fo.veilarbdialog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaRepubliseringService {

    private final DialogDataService dialogDataService;

    private final DialogDAO dialogDAO;

    public void republiserEndringPaaDialogMeldingerForBrukereMedAktivDialog() {
        List<String> aktorIder = dialogDAO.hentAktorIderTilBrukereMedAktiveDialoger();

        log.info("Republiserer meldinger på endring på dialog topic for {} brukere med aktiv dialog.", aktorIder.size());

        aktorIder.forEach(dialogDataService::sendPaaKafka);
    }
}
