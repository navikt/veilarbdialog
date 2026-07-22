package no.nav.fo.veilarbdialog.dialog.exceptions

class AktivitetHarAlleredeDialogTrådException(val aktivitetId: String?): RuntimeException("Aktivitet med id: $aktivitetId har allerede en dialogtråd")
