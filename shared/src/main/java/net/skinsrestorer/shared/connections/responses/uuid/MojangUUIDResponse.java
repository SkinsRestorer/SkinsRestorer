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
package net.skinsrestorer.shared.connections.responses.uuid;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;

@Getter
@SuppressWarnings("unused")
@SuppressFBWarnings({"UWF_UNWRITTEN_FIELD", "URF_UNREAD_FIELD"})
public class MojangUUIDResponse {
    private String name;
    /**
     * UUID without dashes
     */
    private String id;
    private String error;
    private String errorMessage;
}
