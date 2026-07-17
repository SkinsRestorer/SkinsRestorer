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
package net.skinsrestorer.fand.placeholder;

import io.fand.api.Fand;
import io.fand.api.entity.Player;
import io.fand.api.event.player.PlayerJoinEvent;
import io.fand.api.event.player.PlayerQuitEvent;
import io.fand.api.placeholder.PlaceholderProvider;
import io.fand.api.plugin.PluginContext;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.fand.SRFandAdapter;
import net.skinsrestorer.fand.wrapper.WrapperFand;
import net.skinsrestorer.shared.storage.HardcodedSkins;
import org.jspecify.annotations.Nullable;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class FandPlaceholderExpansion {
    private static final String ERROR_MESSAGE = "Error";
    private static final String NAMESPACE = "skinsrestorer";
    private static final String PREFIX = NAMESPACE + "_";
    private static final long CACHE_TTL_NANOS = TimeUnit.SECONDS.toNanos(30);
    private static final TextureData STEVE_TEXTURE = TextureData.from(HardcodedSkins.STEVE.getProperty());
    private static final TextureData ALEX_TEXTURE = TextureData.from(HardcodedSkins.ALEX.getProperty());

    private final PluginContext context;
    private final SRFandAdapter adapter;
    private final WrapperFand wrapper;
    private final ConcurrentHashMap<UUID, CachedSkin> skinNames = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, TextureData> textures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, SkinProperty> requestedTextures = new ConcurrentHashMap<>();
    private final Set<UUID> activePlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> refreshes = ConcurrentHashMap.newKeySet();
    private final Set<UUID> textureRefreshes = ConcurrentHashMap.newKeySet();

    public void register() {
        context.placeholders().register(NAMESPACE, PlaceholderProvider.contextual((placeholderContext, identifier) -> {
            Player player = placeholderContext.targetOptional().orElse(placeholderContext.viewer());
            return resolve(player, identifier);
        }));
        context.events().subscribe(PlayerJoinEvent.class, event -> track(event.player()));
        context.events().subscribe(PlayerQuitEvent.class, event -> untrack(event.player().uniqueId()));
        Fand.server().players().forEach(this::track);
    }

    private @Nullable String resolve(@Nullable Player player, String identifier) {
        String normalized = identifier.toLowerCase(Locale.ROOT);
        if (!normalized.startsWith(PREFIX)) {
            return null;
        }
        String params = normalized.substring(PREFIX.length());
        if (params.startsWith("skin_name")) {
            return resolveSkinName(player, params);
        }
        if (params.startsWith("texture_url")) {
            return resolveTexture(player, params, true);
        }
        if (params.startsWith("texture_id")) {
            return resolveTexture(player, params, false);
        }
        return null;
    }

    private String resolveSkinName(@Nullable Player player, String params) {
        if (player == null) {
            return ERROR_MESSAGE;
        }
        if (!track(player)) {
            return fallbackSkinName(player, params);
        }
        UUID playerId = player.uniqueId();
        CachedSkin cached = skinNames.get(playerId);
        if (cached == null || cached.expired()) {
            refresh(playerId);
        }
        Optional<SkinIdentifier> skin = cached == null ? Optional.empty() : cached.skin();
        if (skin.isPresent()) {
            return skin.orElseThrow().getIdentifier();
        }
        return fallbackSkinName(player, params);
    }

    private static String fallbackSkinName(Player player, String params) {
        return switch (params) {
            case "skin_name_or_empty" -> "";
            case "skin_name_or_player_name" -> player.name();
            default -> ERROR_MESSAGE;
        };
    }

    private String resolveTexture(@Nullable Player player, String params, boolean url) {
        if (player == null) {
            return ERROR_MESSAGE;
        }
        if (!track(player)) {
            return fallbackTexture(params, url);
        }
        Optional<SkinProperty> property = adapter.getSkinProperty(wrapper.player(player));
        if (property.isPresent()) {
            SkinProperty current = property.orElseThrow();
            TextureData cached = textures.get(player.uniqueId());
            if (cached != null && cached.matches(current)) {
                return cached.value(url);
            }
            refreshTexture(player.uniqueId(), current);
        } else {
            clearTexture(player.uniqueId());
        }
        return fallbackTexture(params, url);
    }

    private static String fallbackTexture(String params, boolean url) {
        String suffix = params.substring(url ? "texture_url".length() : "texture_id".length());
        return switch (suffix) {
            case "_or_empty" -> "";
            case "_or_steve" -> STEVE_TEXTURE.value(url);
            case "_or_alex" -> ALEX_TEXTURE.value(url);
            default -> ERROR_MESSAGE;
        };
    }

    private void refresh(UUID playerId) {
        if (!refreshes.add(playerId)) {
            return;
        }
        adapter.runAsync(() -> {
            try {
                refreshNow(playerId);
            } finally {
                refreshes.remove(playerId);
            }
        });
    }

    public void refreshSkinNameAfterChange(Player player) {
        adapter.runAsync(() -> {
            if (track(player)) {
                refreshNow(player.uniqueId());
            }
        });
    }

    public void refreshTextureAfterSkinChange(Player player, SkinProperty property) {
        if (!player.online()) {
            return;
        }
        activePlayers.add(player.uniqueId());
        if (property.getValue().isBlank()) {
            clearTexture(player.uniqueId());
        } else {
            refreshTexture(player.uniqueId(), property);
        }
    }

    private void refreshNow(UUID playerId) {
        Optional<SkinIdentifier> skin = SkinsRestorerProvider.get()
                .getPlayerStorage()
                .getSkinIdOfPlayer(playerId);
        if (activePlayers.contains(playerId)) {
            skinNames.put(playerId, new CachedSkin(skin, System.nanoTime()));
        }
    }

    private boolean track(Player player) {
        if (!player.online()) {
            return false;
        }
        if (activePlayers.add(player.uniqueId())) {
            refresh(player.uniqueId());
            adapter.getSkinProperty(wrapper.player(player))
                    .ifPresent(property -> refreshTexture(player.uniqueId(), property));
        }
        return true;
    }

    private void untrack(UUID playerId) {
        activePlayers.remove(playerId);
        skinNames.remove(playerId);
        clearTexture(playerId);
    }

    private void refreshTexture(UUID playerId, SkinProperty property) {
        requestedTextures.put(playerId, property);
        if (!textureRefreshes.add(playerId)) {
            return;
        }
        adapter.runAsync(() -> {
            SkinProperty requested = requestedTextures.get(playerId);
            try {
                if (requested != null) {
                    TextureData parsed = TextureData.from(requested);
                    if (activePlayers.contains(playerId) && requested.equals(requestedTextures.get(playerId))) {
                        textures.put(playerId, parsed);
                    }
                }
            } catch (RuntimeException invalidTexture) {
                requestedTextures.remove(playerId, requested);
                textures.remove(playerId);
            } finally {
                textureRefreshes.remove(playerId);
                SkinProperty latest = requestedTextures.get(playerId);
                TextureData cached = textures.get(playerId);
                if (activePlayers.contains(playerId)
                        && latest != null
                        && (cached == null || !cached.matches(latest))) {
                    refreshTexture(playerId, latest);
                }
            }
        });
    }

    private void clearTexture(UUID playerId) {
        requestedTextures.remove(playerId);
        textures.remove(playerId);
    }

    private record CachedSkin(Optional<SkinIdentifier> skin, long loadedAtNanos) {
        private boolean expired() {
            return System.nanoTime() - loadedAtNanos >= CACHE_TTL_NANOS;
        }
    }

    private record TextureData(String skinValue, String url, String hash) {
        private static TextureData from(SkinProperty property) {
            var skin = PropertyUtils.getSkinProfileData(property).getTextures().getSKIN();
            return new TextureData(
                    property.getValue(),
                    skin.getUrl(),
                    skin.getTextureHash());
        }

        private boolean matches(SkinProperty property) {
            return skinValue.equals(property.getValue());
        }

        private String value(boolean fullUrl) {
            return fullUrl ? url : hash;
        }
    }
}
