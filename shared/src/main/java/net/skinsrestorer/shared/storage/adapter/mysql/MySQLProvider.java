/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
package net.skinsrestorer.shared.storage.adapter.mysql;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.config.DatabaseConfig;
import net.skinsrestorer.shared.log.SRLogger;
import org.intellij.lang.annotations.Language;
import org.mariadb.jdbc.Configuration;
import org.mariadb.jdbc.pool.Pool;
import org.mariadb.jdbc.pool.Pools;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MySQLProvider {
    private final SRLogger logger;
    private final SettingsManager settings;
    private Pool pool;

    public void initPool() throws SQLException {
        String host = settings.getProperty(DatabaseConfig.MYSQL_HOST);
        String username = settings.getProperty(DatabaseConfig.MYSQL_USERNAME);
        String password = settings.getProperty(DatabaseConfig.MYSQL_PASSWORD);
        String database = settings.getProperty(DatabaseConfig.MYSQL_DATABASE);
        int port = settings.getProperty(DatabaseConfig.MYSQL_PORT);
        int maxPoolSize = settings.getProperty(DatabaseConfig.MYSQL_MAX_POOL_SIZE);
        String options = settings.getProperty(DatabaseConfig.MYSQL_CONNECTION_OPTIONS);

        Configuration configuration = Configuration.parse("jdbc:mysql://" + host + ":" + port + "/" + database +
                "?permitMysqlScheme" +
                "&maxPoolSize=" + maxPoolSize +
                "&" + options);

        pool = Pools.retrievePool(configuration.clone(username, password));
    }

    public void execute(@Language("sql") final String query, final Object... vars) {
        try (Connection connection = pool.getPoolConnection().getConnection()) {
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

            // If there is an error in unit tests, we want to know about it
            assert false;
        }
    }

    public ResultSet query(@Language("sql") final String query, final Object... vars) throws SQLException {
        try (Connection connection = pool.getPoolConnection().getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                fillPreparedStatement(ps, vars);

                return ps.executeQuery();
            }
        }
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
