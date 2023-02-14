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
