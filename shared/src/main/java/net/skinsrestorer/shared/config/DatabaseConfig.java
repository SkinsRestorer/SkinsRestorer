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
package net.skinsrestorer.shared.config;

import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.Property;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;
import static net.skinsrestorer.shared.config.ConfigHelpers.newCappedProperty;

public class DatabaseConfig implements SettingsHolder {
    public static final Property<Boolean> MYSQL_ENABLED = newProperty("database.enabled", false);
    public static final Property<String> MYSQL_HOST = newProperty("database.host", "localhost");
    public static final Property<Integer> MYSQL_PORT = newCappedProperty("database.port", 3306, 1, 65535);
    public static final Property<String> MYSQL_DATABASE = newProperty("database.database", "db");
    public static final Property<String> MYSQL_USERNAME = newProperty("database.username", "root");
    public static final Property<String> MYSQL_PASSWORD = newProperty("database.password", "pass");
    public static final Property<Integer> MYSQL_MAX_POOL_SIZE = newCappedProperty("database.maxPoolSize", 10, 1, 1000);
    public static final Property<String> MYSQL_TABLE_PREFIX = newProperty("database.tablePrefix", "sr_");
    public static final Property<String> MYSQL_CONNECTION_OPTIONS = newProperty("database.connectionOptions", "sslMode=trust&serverTimezone=UTC");

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("database",
                "\n",
                "\n############",
                "\n# Database #",
                "\n############",
                "\n",
                "Settings for databases skin storage (recommended for large networks with a lot of skins)",
                "[!] Make sure you have the correct permissions set for your MySQL user. [!]",
                "[!] Make sure to fill in database.connectionOptions if you're using certificate / ssl authentication. [!]",
                "[!] If you're not using ssl, change sslMode=trust to sslMode=disable [!]"
        );
    }
}
