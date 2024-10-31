package no.nav.fo.veilarbdialog.eskaleringsvarsel

enum class StansVarselBegrunnelseType(
    val tekst: String // Ikke bruk denne til noe, den er bare forklaring av enumen
) {
    dagpenger("Dagpenger: Stans og tidsbegrenset bortfall"),
    dagpenger_vesentlig_avvik_fra_oppleringsplanen("Dagpenger: Vesentlig avvik fra opplæringsplanen."),
    dagpenger_fortsatt_utdanning_etter_opphort_utdanning("Dagpenger: Fortsatt utdanning etter opphørt utdanning."),
    ikke_mott_mote("Arbeidsavklaringspenger: Ikke møtt til møte"),
    ikke_deltatt_aktivitet("Arbeidsavklaringspenger: Ikke deltatt på planlagt aktivitet eller bidrar ikke for å komme i arbeid"),
    ikke_deltatt_tiltak("Arbeidsavklaringspenger: Ikke deltatt på tiltak"),
    ikke_lenger_nedsatt_arbeidsevne("Arbeidsavklaringspenger: Ikke lenger nedsatt arbeidsevne"),
    uutnyttet_arbeidsevne("Arbeidsavklaringspenger: Reduksjon i utbetaling på grunn av arbeidsevne som ikke er utnyttet"),
    stans_aap_i_periode("Arbeidsavklaringspenger: Stans av AAP i perioden som arbeidssøker"),
    overgangsstonad("Overgangsstønad"),
    sykepenger("Sykepenger")
}

