/*
 * SkinsRestorer
 * Copyright (C) 2024  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.shared.storage.adapter.postgresql;

import ch.jalu.configme.SettingsManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.config.DatabaseConfig;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlugin;
import org.intellij.lang.annotations.Language;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PostgreSQLProvider {
    private final SRLogger logger;
    private final SettingsManager settings;
    private HikariDataSource dataSource;

    public void initPool() {
        String host = settings.getProperty(DatabaseConfig.MYSQL_HOST);
        String username = settings.getProperty(DatabaseConfig.MYSQL_USERNAME);
        String password = settings.getProperty(DatabaseConfig.MYSQL_PASSWORD);
        String database = settings.getProperty(DatabaseConfig.MYSQL_DATABASE);
        int port = settings.getProperty(DatabaseConfig.MYSQL_PORT);
        int maxPoolSize = settings.getProperty(DatabaseConfig.MYSQL_MAX_POOL_SIZE);
        String options = settings.getProperty(DatabaseConfig.MYSQL_CONNECTION_OPTIONS);

        StringBuilder jdbcUrlBuilder = new StringBuilder("jdbc:postgresql://")
                .append(host)
                .append(":")
                .append(port)
                .append("/")
                .append(database);

        if (options != null && !options.isBlank()) {
            jdbcUrlBuilder.append("?").append(options);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrlBuilder.toString());
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(maxPoolSize);
        config.setPoolName("SkinsRestorer-PostgreSQL");

        if (dataSource != null) {
            dataSource.close();
        }

        dataSource = new HikariDataSource(config);
    }

    public int update(@Language("sql") final String query, final Object... vars) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            fillPreparedStatement(ps, vars);
            return ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("PostgreSQL error: %s".formatted(e.getMessage()), e);

            if (SRPlugin.isUnitTest()) {
                throw new AssertionError(e);
            }

            return -1;
        }
    }

    public ResultSet query(@Language("sql") final String query, final Object... vars) throws SQLException {
        Connection connection = dataSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(query);
        boolean success = false;
        try {
            fillPreparedStatement(ps, vars);
            ResultSet rs = ps.executeQuery();
            success = true;
            return wrapResultSet(rs, ps, connection);
        } finally {
            if (!success) {
                safeClose(ps);
                safeClose(connection);
            }
        }
    }

    private ResultSet wrapResultSet(ResultSet delegate, PreparedStatement ps, Connection connection) {
        return (ResultSet) Proxy.newProxyInstance(
                delegate.getClass().getClassLoader(),
                new Class[]{ResultSet.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        try {
                            return method.invoke(delegate, args);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        } finally {
                            safeClose(ps);
                            safeClose(connection);
                        }
                    }

                    try {
                        return method.invoke(delegate, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    private void fillPreparedStatement(PreparedStatement ps, Object... vars) throws SQLException {
        for (int i = 0; i < vars.length; i++) {
            int paramIndex = i + 1;
            try {
                ps.setObject(paramIndex, vars[i]);
            } catch (SQLException e) {
                throw new SQLException("Error while setting prepared statement variable #%d (%s): %s".formatted(paramIndex, vars[i], e.getMessage()));
            }
        }
    }

    private void safeClose(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}
