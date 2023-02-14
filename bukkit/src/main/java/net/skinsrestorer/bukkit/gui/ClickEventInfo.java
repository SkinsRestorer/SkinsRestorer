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
package net.skinsrestorer.bukkit.gui;

import com.cryptomorin.xseries.XMaterial;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

@Getter
@RequiredArgsConstructor
public class ClickEventInfo {
    private final XMaterial material;
    private final ItemMeta itemMeta;
    private final Player player;
    private final int currentPage;
}
