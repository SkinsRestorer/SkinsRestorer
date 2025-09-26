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
package net.skinsrestorer.adapter;

import ch.jalu.configme.SettingsManager;
import ch.jalu.injector.Injector;
import net.skinsrestorer.SRExtension;
import net.skinsrestorer.SettingsHelper;
import net.skinsrestorer.shared.config.DatabaseConfig;
import net.skinsrestorer.shared.storage.adapter.postgresql.PostgreSQLAdapter;
import net.skinsrestorer.shared.storage.adapter.postgresql.PostgreSQLProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.Mockito.when;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@ExtendWith({MockitoExtension.class, SRExtension.class})
public class PostgreSQLAdapterTest {
    public static final String DATABASE_NAME = "testdb";
    public static final String USERNAME = "testuser";
    public static final String PASSWORD = "testpassword";

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName(DATABASE_NAME)
            .withUsername(USERNAME)
            .withPassword(PASSWORD);

    @Mock
    private SettingsManager settingsManager;

    @BeforeEach
    public void setup() {
        SettingsHelper.returnDefaultsForAllProperties(settingsManager);
        when(settingsManager.getProperty(DatabaseConfig.MYSQL_HOST)).thenReturn(postgreSQLContainer.getHost());
        when(settingsManager.getProperty(DatabaseConfig.MYSQL_PORT)).thenReturn(postgreSQLContainer.getFirstMappedPort());
        when(settingsManager.getProperty(DatabaseConfig.MYSQL_USERNAME)).thenReturn(postgreSQLContainer.getUsername());
        when(settingsManager.getProperty(DatabaseConfig.MYSQL_PASSWORD)).thenReturn(postgreSQLContainer.getPassword());
        when(settingsManager.getProperty(DatabaseConfig.MYSQL_DATABASE)).thenReturn(postgreSQLContainer.getDatabaseName());

        when(settingsManager.getProperty(DatabaseConfig.MYSQL_CONNECTION_OPTIONS)).thenReturn("sslmode=disable");
    }

    @Test
    public void testLoad(Injector injector) {
        injector.register(SettingsManager.class, settingsManager);
        PostgreSQLProvider provider = injector.getSingleton(PostgreSQLProvider.class);
        provider.initPool();

        PostgreSQLAdapter adapter = injector.getSingleton(PostgreSQLAdapter.class);
        adapter.init();

        AdapterHelper.testAdapter(adapter);
    }
}
