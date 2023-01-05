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
package net.skinsrestorer.shared.utils;

import net.skinsrestorer.shared.reflection.ReflectionUtil;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.connections.ServiceChecker;
import net.skinsrestorer.shared.utils.log.SRLogger;

import java.util.regex.Pattern;

public class SharedMethods {
    private SharedMethods() {
    }

    public static void runServiceCheck(MojangAPI mojangAPI, SRLogger log) {
        ServiceChecker.ServiceCheckResponse response = ServiceChecker.checkServices(mojangAPI);

        if (response.getWorkingUUID() == 0 || response.getWorkingProfile() == 0) {
            log.info("§c[§4Critical§c] ------------------[§2SkinsRestorer §cis §c§l§nOFFLINE§r§c] -------------------------");
            log.info("§c[§4Critical§c] §cPlugin currently can't fetch new skins due to blocked connection!");
            log.info("§c[§4Critical§c] §cSee https://skinsrestorer.net/firewall for steps to resolve your issue!");
            log.info("§c[§4Critical§c] ----------------------------------------------------------------------");
        }
    }

    public static Throwable getRootCause(Throwable throwable) {
        if (throwable.getCause() != null)
            return getRootCause(throwable.getCause());

        return throwable;
    }
}
