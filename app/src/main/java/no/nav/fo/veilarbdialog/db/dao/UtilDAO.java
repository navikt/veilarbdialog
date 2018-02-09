package no.nav.fo.veilarbdialog.db.dao;

import lombok.Cleanup;
import lombok.SneakyThrows;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import static no.nav.sbl.jdbc.Database.hentDato;

@Component
public class UtilDAO {
    JdbcTemplate database;

    public UtilDAO(JdbcTemplate database) {
        this.database = database;
    }

    public Date getTimestampFromDB() {
        String sql;
        if ("H2".equals(getDbName())) {
            sql = "select CURRENT_TIMESTAMP as tidspunkt";
        } else {
            sql = "SELECT CURRENT_TIMESTAMP as tidspunkt FROM DUAL";
        }
        return  database.query(sql, UtilDAO::getSystimestamp);
    }

    private static Date getSystimestamp(ResultSet rs) throws SQLException {
        if(rs.next()) {
            return hentDato(rs, "tidspunkt");
        }
        return hentDato(rs, "tidspunkt");
    }

    @SneakyThrows
    private String getDbName() {
        @Cleanup Connection connection = database.getDataSource().getConnection();
        return connection.getMetaData().getDatabaseProductName();
    }


}
