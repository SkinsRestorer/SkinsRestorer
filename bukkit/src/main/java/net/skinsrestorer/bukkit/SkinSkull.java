package net.skinsrestorer.bukkit;

import com.cryptomorin.xseries.XMaterial;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.bukkit.BukkitHeadAPI;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public class SkinSkull extends ItemStack {
    private final SkinsRestorer plugin;
    private final SRLogger log;

    public void giveSkull(Player player, String name, IProperty property) {
        player.getInventory().addItem(createSkull(name, property));
    }

    private ItemStack createSkull(String name, IProperty property) {
        ItemStack is = XMaterial.PLAYER_HEAD.parseItem();
        SkullMeta sm = (SkullMeta) Objects.requireNonNull(is).getItemMeta();

        List<String> lore = new ArrayList<>();
        lore.add(C.c(Locale.SKINSMENU_SELECT_SKIN));
        Objects.requireNonNull(sm).setDisplayName(name);
        sm.setLore(lore);
        is.setItemMeta(sm);

        try {
            BukkitHeadAPI.setSkull(is, property);
        } catch (Exception e) {
            log.info("ERROR: could not add '" + name + "' to SkinsGUI, skin might be corrupted or invalid!");
            e.printStackTrace();
        }

        return is;
    }
}
