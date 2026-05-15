package no.nav.domain

import no.nav.fo.veilarbdialog.dialog.exceptions.UgyldigDialogInputException

class DialogId(val value: Long) {
    companion object {
        @JvmStatic
        fun fromValueOrThrow(value: String?): DialogId? {
            if (value == null) return null
            if (value.isEmpty() || value.isBlank()) return null
            try {
                return DialogId(value.toLong())
            } catch (e: NumberFormatException) {
                throw UgyldigDialogInputException("DialogId må være et gyldig tall")
            }
        }
    }
}