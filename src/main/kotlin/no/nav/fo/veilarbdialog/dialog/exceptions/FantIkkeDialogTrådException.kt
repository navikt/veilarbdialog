package no.nav.fo.veilarbdialog.dialog.exceptions

class FantIkkeDialogTrådException(val dialogId: String): RuntimeException("Fant ikke dialog med id: $dialogId")