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
package net.skinsrestorer.fand;

import io.fand.api.entity.Player;
import io.fand.api.player.PlayerSkin;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.fand.placeholder.FandPlaceholderExpansion;
import net.skinsrestorer.shared.api.SkinApplierAccess;
import net.skinsrestorer.shared.api.event.EventBusImpl;
import net.skinsrestorer.shared.api.event.SkinApplyEventImpl;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class SkinApplierFand implements SkinApplierAccess<Player> {
    private final SRFandAdapter adapter;
    private final EventBusImpl eventBus;
    private final FandPlaceholderExpansion placeholders;

    @Override
    public void applySkin(Player player, SkinProperty property) {
        if (!player.online()) {
            return;
        }
        adapter.runAsync(() -> {
            SkinApplyEventImpl applyEvent = new SkinApplyEventImpl(player, property);
            eventBus.callEvent(applyEvent);
            placeholders.refreshSkinNameAfterChange(player);
            if (applyEvent.isCancelled()) {
                return;
            }
            SkinProperty applied = applyEvent.getProperty();
            adapter.runSyncToPlayer(
                    adapter.getPlayerForScheduling(player),
                    () -> {
                        if (player.online()) {
                            player.setSkin(applied.getValue().isBlank()
                                    ? null
                                    : new PlayerSkin(applied.getValue(), applied.getSignature()));
                            placeholders.refreshTextureAfterSkinChange(player, applied);
                        }
                    });
        });
    }
}
