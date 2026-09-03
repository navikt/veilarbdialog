package no.nav.fo.veilarbdialog.db.dao

import no.nav.fo.veilarbdialog.domain.Kladd
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import(KladdDAO::class)
class KladdDAOTest: BaseDAOTest() {

    @Autowired
    private lateinit var dao: KladdDAO

    @Test
    fun `Skal kunne lagre en kladd uten at feil kastes`() {
        assertDoesNotThrow {
            dao.upsertKladd(kladd())
        }
    }

    @Test
    fun `Skal kunne lagre kladd på en ny dialogtråd`() {
        assertDoesNotThrow {
            dao.upsertKladd(kladd(dialogId = null))
        }
    }

    @Test
    fun `Skal kunne lagre kladd på en ny dialogtråd som ikke er knyttet til en aktivitet`() {
        assertDoesNotThrow {
            dao.upsertKladd(kladd(dialogId = null, aktivitetId = null))
        }
    }

    @Test
    fun `Skal kunne lagre kladd på en eksisterende dialogtråd som ikke er knyttet til en aktivitet`() {
        assertDoesNotThrow {
            dao.upsertKladd(kladd(dialogId = "1", aktivitetId = null))
        }
    }

    fun kladd(dialogId: String? = "1", aktivitetId: String? = "1") = Kladd(
        dialogId,
        aktivitetId,
        "aktorId",
        "overskrift",
        "tekst",
        "lagtInnAv"
    )
}
