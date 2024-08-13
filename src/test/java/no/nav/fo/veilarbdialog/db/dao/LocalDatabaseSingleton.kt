package no.nav.fo.veilarbdialog.db.dao

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import no.nav.fo.veilarbdialog.db.DataSourceConfig
import java.sql.SQLException

object LocalDatabaseSingleton {
    val postgres = EmbeddedPostgres.start().postgresDatabase.also {
        try {
            it.getConnection().use { connection ->
                connection.prepareStatement("""
              CREATE USER veilarbdialog NOLOGIN;
              GRANT CONNECT on DATABASE postgres to veilarbdialog;
--              GRANT USAGE ON SCHEMA veilarbdialog to veilarbdialog;
              CREATE USER veilarbdialog_midlertidig NOLOGIN;
              GRANT CONNECT on DATABASE postgres to veilarbdialog_midlertidig;
--              GRANT USAGE ON SCHEMA veilarbdialog to veilarbdialog_midlertidig;
            
            """.trimIndent().trim { it <= ' ' }).executeUpdate()
            }
        } catch (e: SQLException) {
            throw RuntimeException(e)
        }
        DataSourceConfig.migrate(it)
    }
}