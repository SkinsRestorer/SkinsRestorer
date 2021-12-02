package net.skinsrestorer.mappings.mapping1_18;

import net.minecraft.server.level.ServerPlayer;
import org.bukkit.entity.Player;

public class Mapping1_18 {
    public static void triggerHealthUpdate(Player player) {
        ServerPlayer craftPlayer = (ServerPlayer) player;
         craftPlayer.resetSentInfo();
    }
}
