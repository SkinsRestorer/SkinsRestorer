package net.skinsrestorer.mappings.shared;

import org.bukkit.entity.Player;

import java.util.function.Function;

public interface IMapping {
    void triggerHealthUpdate(Player player);

    void accept(Player player, Function<ViaPacketData, Boolean> viaFunction);

    String getVersion();
}
