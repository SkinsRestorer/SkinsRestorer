/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
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
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.storage.adapter.mysql.MySQLAdapter;
import net.skinsrestorer.shared.storage.adapter.mysql.MySQLProvider;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private static final MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>("mariadb:11.8")
            .withDatabaseName(DATABASE_NAME)
            .withUsername(USERNAME)
            .withPassword(PASSWORD);

    @Container
    private static final MySQLContainer<?> mySQLContainer = new ProductionDriverMySQLContainer()
            .withDatabaseName(DATABASE_NAME)
            .withUsername(USERNAME)
            .withPassword(PASSWORD);

    private static final class ProductionDriverMySQLContainer extends MySQLContainer<ProductionDriverMySQLContainer> {
        private ProductionDriverMySQLContainer() {
            super("mysql:8.4");
        }

        @Override
        public String getDriverClassName() {
            return "org.mariadb.jdbc.Driver";
        }

        @Override
        public String getJdbcUrl() {
            return super.getJdbcUrl().replace("jdbc:mysql:", "jdbc:mariadb:");
        }
    }

    static Stream<JdbcDatabaseContainer<?>> databases() {
        return Stream.of(mariaDBContainer, mySQLContainer);
    }

    @Mock
    private SettingsManager settingsManager;
    @TempDir
    private Path tempDir;

    void setup(JdbcDatabaseContainer<?> database) {
        SettingsHelper.returnDefaultsForAllProperties(settingsManager);
        when(settingsManager.getProperty(DatabaseConfig.DATABASE_HOST)).thenReturn(database.getHost());
        when(settingsManager.getProperty(DatabaseConfig.DATABASE_PORT)).thenReturn(database.getFirstMappedPort());
        when(settingsManager.getProperty(DatabaseConfig.DATABASE_USERNAME)).thenReturn(database.getUsername());
        when(settingsManager.getProperty(DatabaseConfig.DATABASE_PASSWORD)).thenReturn(database.getPassword());
        when(settingsManager.getProperty(DatabaseConfig.DATABASE_DATABASE)).thenReturn(database.getDatabaseName());

    }

    @ParameterizedTest
    @MethodSource("databases")
    void load(JdbcDatabaseContainer<?> database, Injector injector) throws Exception {
        setup(database);
        assertDoesNotThrow(() -> {
            injector.register(SettingsManager.class, settingsManager);
            SRPlugin plugin = mock(SRPlugin.class);
            when(plugin.getDataFolder()).thenReturn(tempDir);
            injector.register(SRPlugin.class, plugin);

            MySQLProvider provider = injector.getSingleton(MySQLProvider.class);
            provider.initPool();

            MySQLAdapter adapter = injector.getSingleton(MySQLAdapter.class);
            adapter.init();

            AdapterHelper.testAdapter(adapter);
        });
        packagedDriverRunsOnJava8(database);
    }

    private void packagedDriverRunsOnJava8(JdbcDatabaseContainer<?> database) throws Exception {
        String classpath = System.getProperty("skinsrestorer.java8ProbeClasspath") + File.pathSeparator
                + System.getProperty("skinsrestorer.packagedPlugin");
        Process process = new ProcessBuilder(
                System.getProperty("skinsrestorer.java8Executable"), "-cp", classpath,
                "net.skinsrestorer.adapter.Java8DriverProbe",
                database.getJdbcUrl().replace("jdbc:mysql:", "jdbc:mariadb:"), database.getUsername(), database.getPassword())
                .redirectErrorStream(true).start();
        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) process.destroyForcibly();
        assertTrue(finished, "Packaged Java 8 driver timed out");
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), output);
        assertTrue(output.contains("passes on Java 1.8."), output);
    }

}
