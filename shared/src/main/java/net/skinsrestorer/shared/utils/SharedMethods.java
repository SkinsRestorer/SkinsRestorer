/*
 * SkinsRestorer
 *
 * Copyright (C) 2021 SkinsRestorer
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
package net.skinsrestorer.shared.utils;

import net.skinsrestorer.api.reflection.ReflectionUtil;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.MySQL;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.connections.ServiceChecker;
import net.skinsrestorer.shared.utils.log.SRLogger;

import java.io.File;
import java.util.regex.Pattern;

public class SharedMethods {
    private SharedMethods() {
    }

    public static void allowIllegalACFNames() {
        try {
            Class<?> patternClass = Class.forName("co.aikar.commands.ACFPatterns");

            ReflectionUtil.setObject(patternClass, null, "VALID_NAME_PATTERN", Pattern.compile("(.*?)"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void runServiceCheck(MojangAPI mojangAPI, SRLogger log) {
        ServiceChecker checker = new ServiceChecker();
        checker.setMojangAPI(mojangAPI);
        checker.checkServices();
        ServiceChecker.ServiceCheckResponse response = checker.getResponse();

        if (response.getWorkingUUID().get() == 0 || response.getWorkingProfile().get() == 0) {
            log.info("§c[§4Critical§c] ------------------[§2SkinsRestorer §cis §c§l§nOFFLINE§c] -------------------------");
            log.info("§c[§4Critical§c] §cPlugin currently can't fetch new skins due to blocked connection!");
            log.info("§c[§4Critical§c] §cSee http://skinsrestorer.net/firewall for steps to resolve your issue!");
            log.info("§c[§4Critical§c] ----------------------------------------------------------------------");
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean initMysql(SRLogger srLogger, SkinStorage skinStorage, File dataFolder) {
        if (Config.MYSQL_ENABLED) {
            try {
                MySQL mysql = new MySQL(
                        srLogger,
                        Config.MYSQL_HOST,
                        Config.MYSQL_PORT,
                        Config.MYSQL_DATABASE,
                        Config.MYSQL_USERNAME,
                        Config.MYSQL_PASSWORD,
                        Config.MYSQL_CONNECTION_OPTIONS
                );

                mysql.openConnection();
                mysql.createTable();

                skinStorage.setMysql(mysql);
            } catch (Exception e) {
                srLogger.info("§cCan't connect to MySQL! Disabling SkinsRestorer.", e);
                return false;
            }
        } else {
            skinStorage.loadFolders(dataFolder);
        }

        return true;
    }
}
