package net.skinsrestorer.bukkit.gui;

import com.cryptomorin.xseries.XMaterial;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class SkinsGUIHolder implements InventoryHolder {
    private final int page; // Page number start with 0
    private final Consumer<ClickEventInfo> callback;
    @Getter
    @Setter
    private Inventory inventory;

    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        if (!(event.getCurrentItem() == null)) {
            return;
        }

        ItemStack currentItem = event.getCurrentItem();

        // Cancel invalid items
        if (!currentItem.hasItemMeta()) {
            return;
        }

        ItemMeta itemMeta = currentItem.getItemMeta();

        if (itemMeta == null) {
            return;
        }

        callback.accept(new ClickEventInfo(XMaterial.matchXMaterial(currentItem), itemMeta, player, page));
    }
}
