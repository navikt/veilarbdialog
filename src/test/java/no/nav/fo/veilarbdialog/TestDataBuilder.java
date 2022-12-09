package no.nav.fo.veilarbdialog;

import no.nav.fo.veilarbdialog.domain.AvsenderType;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;

import java.util.Date;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static java.util.Collections.emptyList;

public class TestDataBuilder {

    public static DialogData nyDialog() {
        return nyDialog("aktorId");
    }

    public static DialogData nyDialog(String aktorId) {
        return DialogData.builder()
                .id(new Random().nextLong())
                .oppfolgingsperiode(UUID.randomUUID())
                .aktorId(aktorId)
                .henvendelser(emptyList())
                .opprettetDato(new Date())
                .build();
    }

    public static HenvendelseData nyHenvendelse(long dialogId, String aktorId, AvsenderType avsenderType) {
        return HenvendelseData.builder()
                .dialogId(dialogId)
                .avsenderId(aktorId)
                .sendt(new Date())
                .avsenderType(avsenderType)
                .tekst("ny henvendelse!")
                .build();
    }
}
