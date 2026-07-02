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
package net.skinsrestorer.shared.skinsafety;

import org.jetbrains.annotations.Nullable;

public record SkinSafetyDecision(SkinSafetyMode mode, @Nullable SkinSafetyMatch match) {
    public static SkinSafetyDecision allow(SkinSafetyMode mode) {
        return new SkinSafetyDecision(mode, null);
    }

    public boolean matched() {
        return match != null;
    }

    public boolean shouldWarn() {
        return matched() && mode == SkinSafetyMode.WARN;
    }

    public boolean shouldBlock() {
        return matched() && mode == SkinSafetyMode.ENFORCE;
    }
}
