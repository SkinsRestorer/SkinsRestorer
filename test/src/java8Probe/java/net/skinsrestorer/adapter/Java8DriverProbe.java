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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Properties;

/** Exercises the driver loaded only from the packaged plugin on Java 8. */
public final class Java8DriverProbe {
    public static void main(String[] args) throws Exception {
        Driver driver = Class.forName("net.skinsrestorer.shadow.mariadb.Driver").asSubclass(Driver.class)
                .getDeclaredConstructor().newInstance();
        Properties properties = new Properties();
        properties.setProperty("user", args[1]);
        properties.setProperty("password", args[2]);
        // Testcontainers databases use self-signed TLS certificates.
        properties.setProperty("sslMode", "trust");
        try (Connection connection = driver.connect(args[0], properties)) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TEMPORARY TABLE java8_driver_probe (id BIGINT PRIMARY KEY, label VARCHAR(255), value_double DOUBLE, payload BLOB)");
            }
            byte[] payload = new byte[]{0, 1, -1, 127};
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO java8_driver_probe VALUES (?, ?, ?, ?)")) {
                insert.setLong(1, Long.MAX_VALUE);
                insert.setString(2, "Skin ä 日本語");
                insert.setDouble(3, 1234.5);
                insert.setBytes(4, payload);
                if (insert.executeUpdate() != 1) throw new AssertionError("Insert count");
            }
            try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT * FROM java8_driver_probe")) {
                if (!rows.next() || rows.getLong("id") != Long.MAX_VALUE
                        || !"Skin ä 日本語".equals(rows.getString("label"))
                        || Double.compare(rows.getDouble("value_double"), 1234.5) != 0
                        || !Arrays.equals(payload, rows.getBytes("payload")) || rows.next()) {
                    throw new AssertionError("Packaged driver round trip");
                }
            }
            try (Statement statement = connection.createStatement()) {
                if (statement.executeUpdate("DELETE FROM java8_driver_probe") != 1) throw new AssertionError("Delete count");
            }
        }
        System.out.println("Packaged MariaDB driver passes on Java " + System.getProperty("java.version"));
    }
}
