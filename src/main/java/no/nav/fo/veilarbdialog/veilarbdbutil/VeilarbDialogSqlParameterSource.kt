package no.nav.veilarbaktivitet.veilarbdbutil

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import java.time.ZonedDateTime

class VeilarbDialogSqlParameterSource(private val params: MapSqlParameterSource = MapSqlParameterSource()): SqlParameterSource by params {
    @Override
    fun addValue(paramName: String, value: Any?): VeilarbDialogSqlParameterSource {
        val castedValue = when {
            value is Boolean -> if (value) 1 else 0
            value is ZonedDateTime -> value.toOffsetDateTime()
            else -> value
        }
        params.addValue(paramName, castedValue)
        return this
    }
}