package no.nav.fo.veilarbdialog;

import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;

import java.util.Collections;
import java.util.Random;

import static java.util.Collections.emptyList;

public class TestDataBuilder {

    public static DialogData nyDialog(String aktorId) {
        return DialogData.builder()
                .id(new Random().nextLong())
                .aktorId(aktorId)
                .henvendelser(emptyList())
                .build();
    }

    public static HenvendelseData nyHenvendelse(long dialogId) {
        return HenvendelseData.builder()
                .dialogId(dialogId)
                .build();
    }

}
