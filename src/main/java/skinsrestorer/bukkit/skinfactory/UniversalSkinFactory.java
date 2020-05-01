package skinsrestorer.bukkit.skinfactory;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class UniversalSkinFactory extends SkinFactory {
    private final Plugin plugin;
    private final Consumer<Player> refresh = detectRefresh();

    @SuppressWarnings("deprecation")
    @Override
    public void updateSkin(Player player) {
        if (!player.isOnline())
            return;
        val plugin = this.plugin;
        for (Player ps : Bukkit.getOnlinePlayers()) {
            ps.hidePlayer(plugin, player);
            ps.showPlayer(plugin, player);
        }
        refresh.accept(player);
    }

    private static Consumer<Player> detectRefresh() {
        if (Bukkit.getName().toLowerCase().contains("paper")) {
            return new PaperSkinRefresher();
        }
        return new LegacySkinRefresher();
    }
}
