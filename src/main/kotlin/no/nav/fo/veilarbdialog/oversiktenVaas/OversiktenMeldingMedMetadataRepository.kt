package no.nav.fo.veilarbdialog.oversiktenVaas

import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.db.jdbc.JdbcConverters.toLocalDateTime
import no.nav.fo.veilarbdialog.db.jdbc.JdbcConverters.toZonedDateTime
import no.nav.fo.veilarbdialog.db.jdbc.OversiktenJdbcRepository
import no.nav.fo.veilarbdialog.db.jdbc.OversiktenMeldingRow
import no.nav.fo.veilarbdialog.util.DatabaseUtils
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
open class OversiktenMeldingMedMetadataRepository(
    private val repository: OversiktenJdbcRepository
) {
    open fun lagre(oversiktenMeldingMedMetadata: OversiktenMeldingMedMetadata): Long {
        return repository.insertAndReturnId(
            oversiktenMeldingMedMetadata.fnr.get(),
            toLocalDateTime(oversiktenMeldingMedMetadata.opprettet),
            oversiktenMeldingMedMetadata.utsendingStatus.name,
            oversiktenMeldingMedMetadata.meldingSomJson,
            oversiktenMeldingMedMetadata.kategori.name,
            oversiktenMeldingMedMetadata.meldingKey,
            oversiktenMeldingMedMetadata.operasjon.name
        )
    }

    open fun hentAlleSomSkalSendes(): List<LagretOversiktenMeldingMedMetadata> {
        return repository.findAlleSomSkalSendes().map { it.toDomain() }
    }

    open fun hent(meldingKey: MeldingKey, operasjon: OversiktenMelding.Operasjon): List<LagretOversiktenMeldingMedMetadata> {
        return repository.findByMeldingKeyAndOperasjon(meldingKey, operasjon.name).map { it.toDomain() }
    }

    open fun hent(id: Long): LagretOversiktenMeldingMedMetadata {
        return repository.findRowById(id).map { it.toDomain() }
            .orElseThrow { IllegalStateException("Fant ingen oversikten-melding med id $id") }
    }

    open fun markerSomSendt(id: Long) {
        repository.markerSomSendt(id)
    }

    /** Kept for test support code that maps raw query results. */
    open val rowMapper = RowMapper { rs: ResultSet, _: Int ->
        LagretOversiktenMeldingMedMetadata(
            id = rs.getLong("id"),
            fnr = Fnr.of(rs.getString("fnr")),
            opprettet = DatabaseUtils.hentZonedDateTime(rs, "opprettet"),
            utsendingStatus = UtsendingStatus.valueOf(rs.getString("utsending_status")),
            meldingSomJson = rs.getString("melding"),
            kategori = OversiktenMelding.Kategori.valueOf(rs.getString("kategori")),
            meldingKey = java.util.UUID.fromString(rs.getString("melding_key")),
            tidspunktSendt = DatabaseUtils.hentZonedDateTime(rs, "tidspunkt_sendt"),
            operasjon = OversiktenMelding.Operasjon.valueOf(rs.getString("operasjon")),
        )
    }

    private fun OversiktenMeldingRow.toDomain() = LagretOversiktenMeldingMedMetadata(
        id = id,
        fnr = Fnr.of(fnr),
        opprettet = toZonedDateTime(opprettet),
        utsendingStatus = UtsendingStatus.valueOf(utsendingStatus),
        meldingSomJson = meldingSomJson,
        kategori = OversiktenMelding.Kategori.valueOf(kategori),
        meldingKey = meldingKey,
        tidspunktSendt = toZonedDateTime(tidspunktSendt),
        operasjon = OversiktenMelding.Operasjon.valueOf(operasjon),
    )
}
