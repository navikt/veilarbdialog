package no.nav.fo.veilarbdialog.auth

import no.nav.common.types.identer.EksternBrukerId
import no.nav.poao.dab.spring_auth.IAuthService
import no.nav.poao.dab.spring_auth.TilgangsType

fun IAuthService.sjekkTilgangTilPersonOgAuditlog(ident: EksternBrukerId, tilgangsType: TilgangsType, auditlogMessage: String) {
    val subject = getLoggedInnUser()
    try {
        sjekkTilgangTilPerson(ident, tilgangsType)
    } catch (e: Exception) {
        auditlog(false, subject, ident, auditlogMessage)
        throw e
    }
    auditlog(true, subject, ident, auditlogMessage)
}
