package no.nav.fo.veilarbdialog.dialog

class FantIkkeDialogTrådException(val dialogId: String): Exception("Fant ikke dialog med id: $dialogId")