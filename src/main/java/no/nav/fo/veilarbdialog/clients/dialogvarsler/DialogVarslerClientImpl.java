package no.nav.fo.veilarbdialog.clients.dialogvarsler;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.clients.util.HttpClientWrapper;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DialogVarslerClientImpl implements DialogVarslerClient {
    private final HttpClientWrapper dialogvarslerClientWrapper;

    public DialogVarslerClientImpl(HttpClientWrapper dialogvarslerClientWrapper) {
        this.dialogvarslerClientWrapper = dialogvarslerClientWrapper;
    }

    @Override
    public void varsleLyttere(Fnr fnr) {
        try {
            var payload = new DialogVarselDto(fnr.get(), "NY_DIALOGMELDING_FRA_BRUKER_TIL_NAV");
            var requestBody = RequestBody.create(JsonUtils.toJson(payload), MediaType.parse("application/json"));
            dialogvarslerClientWrapper.post("/notify-subscribers", requestBody);
        } catch (Exception e) {
            log.warn("Kunne ikke varsle om dialog til PLEASE", e);
        }
    }
}

