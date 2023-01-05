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
package net.skinsrestorer.shared.config;

import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.Property;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public class DatabaseConfig implements SettingsHolder {
    public static final Property<Boolean> MYSQL_ENABLED = newProperty("MySQL.Enabled", false);
    public static final Property<String> MYSQL_HOST = newProperty("MySQL.Host", "localhost");
    public static final Property<Integer> MYSQL_PORT = newProperty("MySQL.Port", 3306);
    public static final Property<String> MYSQL_DATABASE = newProperty("MySQL.Database", "db");
    public static final Property<String> MYSQL_USERNAME = newProperty("MySQL.Username", "root");
    public static final Property<String> MYSQL_PASSWORD = newProperty("MySQL.Password", "pass");
    public static final Property<Integer> MYSQL_MAX_POOL_SIZE = newProperty("MySQL.MaxPoolSize", 10);
    public static final Property<String> MYSQL_SKIN_TABLE = newProperty("MySQL.SkinTable", "Skins");
    public static final Property<String> MYSQL_PLAYER_TABLE = newProperty("MySQL.PlayerTable", "Players");
    public static final Property<String> MYSQL_CONNECTION_OPTIONS = newProperty("MySQL.ConnectionOptions", "sslMode=trust&serverTimezone=UTC");

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("MySQL",
                "Settings for MySQL skin storage (recommended for big BungeeCord networks)",
                "[!] IF YOU USE BUNGEE, DO NOT ENABLE MYSQL in the Spigot / backend config.yml [!]",
                "[!] Non-root users: MySQL 8's new default authentication is not supported, use mysql_native_password [!]",
                "[!] Make sure you have the correct permissions set for your MySQL user. [!]",
                "[!] Make sure to fill in MySQL.ConnectionOptions if you're using certificate / ssl authentication. [!]",
                "[!] If you're not using ssl, change sslMode=trust to sslMode=disable [!]"
        );
    }
}
