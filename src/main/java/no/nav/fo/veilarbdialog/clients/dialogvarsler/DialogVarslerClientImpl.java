package no.nav.fo.veilarbdialog.clients.dialogvarsler;

import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.clients.util.HttpClientWrapper;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.springframework.stereotype.Service;

@Service
public class DialogVarslerClientImpl implements DialogVarslerClient {
    private final HttpClientWrapper dialogvarslerClientWrapper;

    public DialogVarslerClientImpl(HttpClientWrapper dialogvarslerClientWrapper) {
        this.dialogvarslerClientWrapper = dialogvarslerClientWrapper;
    }

    @Override
    public void varsleLyttere(Fnr fnr) {
        var payload = new DialogVarselDto(fnr.get(), "NY_MELDING");
        var requestBody = RequestBody.create(JsonUtils.toJson(payload), MediaType.parse("application/json"));
        dialogvarslerClientWrapper.post("/notifiy-subscribers", requestBody);
    }
}

