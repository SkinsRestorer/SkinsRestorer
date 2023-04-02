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
package net.skinsrestorer.shared.subjects;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Permissions {
    public static final Permission EMPTY = Permission.of("");
    public static final Permission SKIN = Permission.of("skinsrestorer.command");
    public static final Permission SR = Permission.of("skinsrestorer.admincommand");
    public static final Permission SKINS = Permission.of("skinsrestorer.command.gui");
    public static final Permission SKIN_SET = Permission.of("skinsrestorer.command.set");
    public static final Permission SKIN_SET_OTHER = Permission.of("skinsrestorer.command.set.other");
    public static final Permission SKIN_SET_URL = Permission.of("skinsrestorer.command.set.url");
    public static final Permission SKIN_CLEAR = Permission.of("skinsrestorer.command.clear");
    public static final Permission SKIN_CLEAR_OTHER = Permission.of("skinsrestorer.command.clear.other");
    public static final Permission SKIN_SEARCH = Permission.of("skinsrestorer.command.search");
    public static final Permission SKIN_UPDATE = Permission.of("skinsrestorer.command.update");
    public static final Permission SKIN_UPDATE_OTHER = Permission.of("skinsrestorer.command.update.other");
    public static final Permission SR_RELOAD = Permission.of("skinsrestorer.admincommand.reload");
    public static final Permission SR_STATUS = Permission.of("skinsrestorer.admincommand.status");
    public static final Permission SR_DROP = Permission.of("skinsrestorer.admincommand.drop");
    public static final Permission SR_PROPS = Permission.of("skinsrestorer.admincommand.props");
    public static final Permission SR_APPLY_SKIN = Permission.of("skinsrestorer.admincommand.applyskin");
    public static final Permission SR_CREATE_CUSTOM = Permission.of("skinsrestorer.admincommand.createcustom");
    public static final Permission SR_DUMP = Permission.of("skinsrestorer.admincommand.dump");

    public static final Permission BYPASS_COOLDOWN = Permission.of("skinsrestorer.bypasscooldown");
    public static final Permission BYPASS_DISABLED = Permission.of("skinsrestorer.bypassdisabled");
    public static final Permission OWN_SKIN = Permission.of("skinsrestorer.ownskin");

    public static Permission forSkin(String skinName) {
        return Permission.of("skinsrestorer.skin." + skinName);
    }
}
