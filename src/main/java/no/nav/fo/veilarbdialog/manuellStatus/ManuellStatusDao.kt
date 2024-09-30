package no.nav.fo.veilarbdialog.manuellStatus

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service

@Service
class ManuellStatusDao(val jdbcTemplate: NamedParameterJdbcTemplate) {
    fun upsertManuellStatus(manuellStatus: ManuellStatusDto) {
        jdbcTemplate.update("""
            INSERT INTO MANUELL_STATUS VALUES (:aktorId, :erManuell)
            ON CONFLICT SET erManuell = :erManuell
        """.trimIndent(), mapOf("aktorId" to manuellStatus.aktorId, "erManuell" to manuellStatus.erManuell)
        )
    }

    fun getManuellStatus(aktorId: String): ManuellStatusDto {
        return jdbcTemplate.queryForObject(
            "SELECT * FROM MANUELL_STATUS WHERE aktorId = :aktorId",
            mapOf("aktorId" to aktorId)) {
                rs, _ -> ManuellStatusDto(
                rs.getString("aktorId"),
                rs.getBoo("erManuell")
            )
        } ?: ManuellStatusDto(aktorId, false)
    }
}