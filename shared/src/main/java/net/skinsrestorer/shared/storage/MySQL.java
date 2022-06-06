/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.skinsrestorer.shared.storage;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.intellij.lang.annotations.Language;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@RequiredArgsConstructor
public class MySQL {
    private final SRLogger logger;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final int maxPoolSize;
    private final String options;
    private MariaDbPoolDataSource poolDataSource;

    public void createTable() {
        execute("CREATE TABLE IF NOT EXISTS `" + Config.MYSQL_PLAYER_TABLE + "` ("
                + "`Nick` varchar(16) COLLATE utf8_unicode_ci NOT NULL,"
                + "`Skin` varchar(19) COLLATE utf8_unicode_ci NOT NULL,"
                + "PRIMARY KEY (`Nick`)) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci");

        execute("CREATE TABLE IF NOT EXISTS `" + Config.MYSQL_SKIN_TABLE + "` ("
                + "`Nick` varchar(19) COLLATE utf8_unicode_ci NOT NULL,"
                + "`Value` text COLLATE utf8_unicode_ci,"
                + "`Signature` text COLLATE utf8_unicode_ci,"
                + "`timestamp` text COLLATE utf8_unicode_ci,"
                + "PRIMARY KEY (`Nick`)) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci");

        if (!columnExists(Config.MYSQL_SKIN_TABLE, "timestamp")) {
            execute("ALTER TABLE `" + Config.MYSQL_SKIN_TABLE + "` ADD `timestamp` text COLLATE utf8_unicode_ci;");
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        try (Connection connection = poolDataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            return resultSet.getInt(1) > 0;
        } catch (SQLException e) {
            logger.severe("Error checking if column exists", e);
            return false;
        }
    }

    public void connectPool() throws SQLException {
        poolDataSource = new MariaDbPoolDataSource(
                "jdbc:mysql://" + host + ":" + port + "/" + database +
                        "?user=" + username +
                        "&password=" + password +
                        "&permitMysqlScheme" +
                        "&maxPoolSize=" + maxPoolSize +
                        "&" + options);

        logger.info("Connected to MySQL!");
    }

    public void execute(@Language("sql") final String query, final Object... vars) {
        try (Connection connection = poolDataSource.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                fillPreparedStatement(ps, vars);

                ps.execute();
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1060) {
                return;
            }
            e.printStackTrace();
            logger.warning("MySQL error: " + e.getMessage());
        }
    }

    public CachedRowSet query(@Language("sql") final String query, final Object... vars) {
        try (Connection connection = poolDataSource.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                fillPreparedStatement(ps, vars);

                try (ResultSet rs = ps.executeQuery()) {
                    CachedRowSet crs = RowSetProvider.newFactory().createCachedRowSet();
                    crs.populate(rs);

                    if (crs.next())
                        return crs;
                }
            }
        } catch (SQLException e) {
            logger.warning("MySQL error: " + e.getMessage());
        }

        return null;
    }

    private void fillPreparedStatement(PreparedStatement ps, Object... vars) throws SQLException {
        int i = 0;
        for (Object obj : vars) {
            i++;
            try {
                ps.setObject(i, obj);
            } catch (SQLException e) {
                throw new SQLException("Error while setting prepared statement variable #" + i + " (" + obj + "): " + e.getMessage());
            }
        }
    }
}
