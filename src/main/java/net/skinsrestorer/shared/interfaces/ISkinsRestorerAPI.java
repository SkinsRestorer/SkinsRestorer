/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
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
 * #L%
 */
package net.skinsrestorer.shared.interfaces;

import net.skinsrestorer.shared.exception.SkinRequestException;
import net.skinsrestorer.shared.utils.PlayerWrapper;

/**
 * Created by McLive on 27.08.2019.
 *
 *  @param <P> Platform specific Player
 */
public interface ISkinsRestorerAPI<P> {
    Object getProfile(String uuid);

    String getSkinName(String playerName);

    void setSkin(String playerName, String skinName) throws SkinRequestException;

    void applySkin(PlayerWrapper player, Object props);
    void applySkin(PlayerWrapper player);
}
