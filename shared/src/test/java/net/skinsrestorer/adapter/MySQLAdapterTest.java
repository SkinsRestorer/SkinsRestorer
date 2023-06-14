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
package net.skinsrestorer.adapter;

import ch.jalu.configme.SettingsManager;
import ch.jalu.injector.Injector;
import net.skinsrestorer.SRExtension;
import net.skinsrestorer.SettingsHelper;
import net.skinsrestorer.shared.config.DatabaseConfig;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.storage.adapter.mysql.MySQLAdapter;
import net.skinsrestorer.shared.storage.adapter.mysql.MySQLProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.sql.SQLException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@ExtendWith({MockitoExtension.class, SRExtension.class})
public class MySQLAdapterTest {
    public static final String DATABASE_NAME = "testdb";
    public static final String USERNAME = "testuser";
    public static final String PASSWORD = "testpassword";

    @Container
    private static final MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>("mariadb:latest")
            .withDatabaseName(DATABASE_NAME)
            .withUsername(USERNAME)
            .withPassword(PASSWORD);

    @Mock
    private SettingsManager settingsManager;
    @TempDir
    private Path tempDir;

    @BeforeEach
    public void setup() {
        SettingsHelper.returnDefaultsForAllProperties(settingsManager);
        when(settingsManager.getProperty(DatabaseConfig.MYSQL_HOST)).thenReturn(mariaDBContainer.getHost());
        when(settingsManager.getProperty(DatabaseConfig.MYSQL_PORT)).thenReturn(mariaDBContainer.getFirstMappedPort());
        when(settingsManager.getProperty(DatabaseConfig.MYSQL_USERNAME)).thenReturn(mariaDBContainer.getUsername());
        when(settingsManager.getProperty(DatabaseConfig.MYSQL_PASSWORD)).thenReturn(mariaDBContainer.getPassword());
        when(settingsManager.getProperty(DatabaseConfig.MYSQL_DATABASE)).thenReturn(mariaDBContainer.getDatabaseName());

        when(settingsManager.getProperty(DatabaseConfig.MYSQL_CONNECTION_OPTIONS)).thenReturn("sslMode=disable&serverTimezone=UTC");
    }

    @Test
    public void testLoad(Injector injector) throws SQLException {
        injector.register(SettingsManager.class, settingsManager);
        SRPlugin plugin = mock(SRPlugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir);
        injector.register(SRPlugin.class, plugin);

        MySQLProvider provider = injector.getSingleton(MySQLProvider.class);
        provider.initPool();

        MySQLAdapter adapter = injector.getSingleton(MySQLAdapter.class);
        adapter.init();

        AdapterHelper.testAdapter(adapter);
    }
}
