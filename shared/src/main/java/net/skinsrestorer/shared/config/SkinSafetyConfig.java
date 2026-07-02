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
package net.skinsrestorer.shared.config;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.Property;
import net.skinsrestorer.shared.skinsafety.SkinSafetyMode;

import java.util.List;

import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;
import static net.skinsrestorer.shared.config.ConfigHelpers.newCappedProperty;

public class SkinSafetyConfig implements SettingsHolder {
    @Comment({
            "Skin Safety blocks or warns about inappropriate skins using content identifiers instead of only skin names.",
            "This can check exact Mojang texture hashes, player identities, URLs, and optional PNG-based hashes.",
            "The hosted blocklist is optional and disabled by default until you configure an endpoint."
    })
    public static final Property<Boolean> ENABLED = newProperty("skinSafety.enabled", false);
    @Comment("ENFORCE replaces blocked skins with the configured fallback. WARN logs and warns without blocking. AUDIT only logs matches.")
    public static final Property<SkinSafetyMode> MODE = newProperty(SkinSafetyMode.class, "skinSafety.mode", SkinSafetyMode.ENFORCE);
    @Comment("Skin used when ENFORCE blocks a player skin. This should point to a known safe preset such as 'steve' or another reviewed skin.")
    public static final Property<String> FALLBACK_SKIN = newProperty("skinSafety.fallbackSkin", "steve");
    @Comment("Players with this permission bypass command-side Skin Safety checks. Login-time safety checks still protect other players on the server.")
    public static final Property<String> BYPASS_PERMISSION = newProperty("skinSafety.bypassPermission", "skinsrestorer.skinsafety.bypass");
    @Comment("Maximum Hamming distance for perceptual-v1 digest matches. Lower values are stricter; 0 means only exact perceptual matches are blocked.")
    public static final Property<Integer> PERCEPTUAL_HASH_MAX_DISTANCE = newCappedProperty("skinSafety.perceptualHashMaxDistance", 6, 0, 256);

    @Comment("Convenience denylist for obvious aliases, recommendation ids, or custom skin names. Exact texture hashes are still the stronger control.")
    public static final Property<List<String>> LOCAL_BLOCKED_NAMES = newListProperty("skinSafety.local.blockedNames", "hitler", "nudegirl", "swastika");
    @Comment("Preferred primary block key. This is the exact textures.minecraft.net hash extracted from the skin property.")
    public static final Property<List<String>> LOCAL_BLOCKED_TEXTURE_HASHES = newListProperty("skinSafety.local.blockedTextureHashes",
            "cb50beab76e56472637c304a54b330780e278decb017707bf7604e484e4d6c9f");
    @Comment("Secondary rule for premium players that are known to use abusive skins.")
    public static final Property<List<String>> LOCAL_BLOCKED_PLAYER_UUIDS = newListProperty("skinSafety.local.blockedPlayerUuids");
    @Comment("Secondary rule for premium player names. Less stable than UUIDs or texture hashes because players can rename.")
    public static final Property<List<String>> LOCAL_BLOCKED_PLAYER_NAMES = newListProperty("skinSafety.local.blockedPlayerNames");
    @Comment("Block uploaded skin URLs before they are generated. Useful for disposable or known-bad hosts.")
    public static final Property<List<String>> LOCAL_BLOCKED_URL_PREFIXES = newListProperty("skinSafety.local.blockedUrlPrefixes", "https://badhost.example/");
    @Comment("Block entire image hosts by domain name. Subdomains are also matched.")
    public static final Property<List<String>> LOCAL_BLOCKED_DOMAINS = newListProperty("skinSafety.local.blockedDomains", "badhost.example");
    @Comment({
            "Generic image digest rules in the form '<algorithm>:<digest>'.",
            "Supported algorithms: 'sha256' for exact normalized PNG matches and 'perceptual-v1' for near-duplicate matching.",
            "Admin-curated PNGs in skin-safety/blocked add both digest types automatically at runtime."
    })
    public static final Property<List<String>> LOCAL_BLOCKED_DIGESTS = newListProperty("skinSafety.local.blockedDigests");
    @Comment("Local allowlist that wins over the hosted or local texture blocklists.")
    public static final Property<List<String>> LOCAL_ALLOWED_TEXTURE_HASHES = newListProperty("skinSafety.local.allowedTextureHashes");
    @Comment("Local allowlist for player UUIDs that should never be blocked by Skin Safety.")
    public static final Property<List<String>> LOCAL_ALLOWED_PLAYER_UUIDS = newListProperty("skinSafety.local.allowedPlayerUuids");

    @Comment("Enable downloading a hosted blocklist snapshot from a remote moderation service.")
    public static final Property<Boolean> PUBLIC_BLOCKLIST_ENABLED = newProperty("skinSafety.publicBlocklist.enabled", false);
    @Comment("HTTPS endpoint returning a Skin Safety JSON snapshot.")
    public static final Property<String> PUBLIC_BLOCKLIST_URL = newProperty("skinSafety.publicBlocklist.url", "https://skinsafety.skinsrestorer.net/blocklist-v1.json");
    @Comment("How often the plugin refreshes the hosted blocklist.")
    public static final Property<Integer> PUBLIC_BLOCKLIST_REFRESH_MINUTES = newCappedProperty("skinSafety.publicBlocklist.refreshMinutes", 360, 5, Integer.MAX_VALUE);
    @Comment("Keep using the last downloaded blocklist if the remote service is temporarily unavailable.")
    public static final Property<Boolean> PUBLIC_BLOCKLIST_USE_CACHED_COPY = newProperty("skinSafety.publicBlocklist.useCachedCopyOnFailure", true);
    @Comment("Only apply hosted entries from these moderation categories. Leave empty to accept all categories.")
    public static final Property<List<String>> PUBLIC_BLOCKLIST_CATEGORIES = newListProperty("skinSafety.publicBlocklist.categories", "nudity", "hate", "extremist");
    @Comment("Only apply hosted entries at or above this severity value.")
    public static final Property<Integer> PUBLIC_BLOCKLIST_MIN_SEVERITY = newCappedProperty("skinSafety.publicBlocklist.minimumSeverity", 0, 0, Integer.MAX_VALUE);

    @Comment("Optional moderation endpoint for /skin report submissions. Leave empty to keep reports local only.")
    public static final Property<String> REPORT_REMOTE_URL = newProperty("skinSafety.reporting.remoteUrl", "");

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("skinSafety",
                "\n",
                "\n#################",
                "\n# Skin Safety   #",
                "\n#################",
                "\n",
                "Protect players from malicious or inappropriate skins.",
                "The strongest rules match exact content identifiers such as Mojang texture hashes.",
                "Name-based blocking still exists, but it should be treated as a convenience layer rather than the primary control."
        );
        conf.setComment("skinSafety.local",
                "Local rules owned by this server.",
                "Exact texture hashes are the preferred block key because they survive player renames and custom aliases."
        );
        conf.setComment("skinSafety.publicBlocklist",
                "Optional hosted public blocklist. Cached copies are stored under the plugin data folder.",
                "Entries can be filtered by category and minimum severity."
        );
        conf.setComment("skinSafety.reporting",
                "Reports are always written to a local JSONL file.",
                "Set a remoteUrl if you also want the plugin to POST reports to a moderation endpoint."
        );
    }
}
