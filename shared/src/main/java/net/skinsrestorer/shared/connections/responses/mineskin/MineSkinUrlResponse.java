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
package net.skinsrestorer.shared.connections.responses.mineskin;

import com.google.gson.annotations.SerializedName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;

@Getter
@SuppressWarnings("unused")
@SuppressFBWarnings({"UWF_UNWRITTEN_FIELD", "URF_UNREAD_FIELD"})
public class MineSkinUrlResponse {
    private String id;
    private String idStr;
    private String uuid;
    private String name;
    private String variant;
    private MineSkinData data;
    private long timestamp;
    private int duration;
    // sometimes account is returned as a string and sometimes as an int,
    // so it's just easier to comment this field out, it's not used anyway
    // private int account;
    private String server;
    @SerializedName("private")
    private boolean private_;
    private int views;
    private int nextRequest;
    private boolean duplicate;
}
