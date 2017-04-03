package no.nav.fo.veilarbdialog;

import no.nav.fo.veilarbdialog.domain.*;

import java.util.Date;
import java.util.Random;

import static java.util.Arrays.asList;
import static java.util.Calendar.SECOND;
import static org.apache.commons.lang3.time.DateUtils.truncate;

public class AktivitetDataBuilder {

    public static AktivitetData nyAktivitet(String aktorId) {
        return new AktivitetData()
                .setId(new Random().nextLong())
                .setAktorId(aktorId)
                .setFraDato(nyDato())
                .setTilDato(nyDato())
                .setTittel("tittel")
                .setBeskrivelse("beskrivelse")
                .setStatus(AktivitetStatus.values()[0])
                .setAvsluttetDato(nyDato())
                .setAvsluttetKommentar("avsluttetKommentar")
                .setLagtInnAv(InnsenderData.values()[0])
                .setOpprettetDato(nyDato())
                .setLenke("lenke");
    }

    public static StillingsoekAktivitetData nyttStillingssøk() {
        return new StillingsoekAktivitetData()
                .setArbeidsgiver("arbeidsgiver")
                .setKontaktPerson("kontaktperson")
                .setStillingsTittel("stilingstittel")
                .setStillingsoekEtikett(StillingsoekEtikettData.values()[0]);
    }

    public static Date nyDato() {
        return truncate(new Date(new Random().nextLong() % System.currentTimeMillis()), SECOND);
    }


}
