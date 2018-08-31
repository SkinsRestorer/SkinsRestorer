package skinsrestorer.bukkit;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.ReflectionUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SkinsGUI extends ItemStack implements Listener {

    private static ConcurrentHashMap<String, Integer> openedMenus = new ConcurrentHashMap<>();

    public SkinsGUI() {
    }

    public static Inventory getGUI(int page) {
        Inventory inventory = Bukkit.createInventory(null, 54, "§9Skins Menu - Page " + page);
        int skinNumber = 36 * page;
        Map<String, Object> skinsList = SkinStorage.getSkins(skinNumber);
        inventory.setItem(36, createGlass(0));
        inventory.setItem(37, createGlass(0));
        inventory.setItem(38, createGlass(0));
        inventory.setItem(39, createGlass(0));
        inventory.setItem(40, createGlass(0));
        inventory.setItem(41, createGlass(0));
        inventory.setItem(42, createGlass(0));
        inventory.setItem(43, createGlass(0));
        inventory.setItem(44, createGlass(0));

        //Middle button //remove skin
        inventory.setItem(48, createGlass(14));
        inventory.setItem(49, createGlass(14));
        inventory.setItem(50, createGlass(14));
        //button place next
        inventory.setItem(45, createGlass(15));
        inventory.setItem(46, createGlass(15));
        inventory.setItem(47, createGlass(15));

        //button place next
        inventory.setItem(53, createGlass(15));
        inventory.setItem(52, createGlass(15));
        inventory.setItem(51, createGlass(15));

        for (String s : skinsList.keySet()) {
            if (skinsList.get(s) != null) {

                //if page is not 0, adding Previous Page button.
                if (page != 0) {
                    inventory.setItem(45, createGlass(4));
                    inventory.setItem(46, createGlass(4));
                    inventory.setItem(47, createGlass(4));
                }
                //if the page is full, adding Next Page button.
                if (inventory.firstEmpty() == -1) {
                    inventory.setItem(53, createGlass(5));
                    inventory.setItem(52, createGlass(5));
                    inventory.setItem(51, createGlass(5));
                    continue;
                }
                inventory.addItem(createSkull(skinsList.get(s), s));
            }
        }
        return inventory;
    }

    private static ItemStack createGlass(int color) {
        ItemStack is = new ItemStack(Material.WHITE_STAINED_GLASS_PANE, 1, (short) 3);
        ItemMeta meta = is.getItemMeta();
        if (color == 5) {
            meta.setDisplayName(Locale.NEXT_PAGE);
        } else if (color == 4) {
            meta.setDisplayName(Locale.PREVIOUS_PAGE);
        } else if (color == 14) {
            meta.setDisplayName(Locale.REMOVE_SKIN);
        } else {
            meta.setDisplayName(" ");
        }
        is.setItemMeta(meta);
        is.setDurability((short) color);
        return is;
    }

    private static ItemStack createSkull(Object s, String name) {
        ItemStack is = new ItemStack(Material.SKELETON_SKULL, 1, (short) 3);
        SkullMeta sm = (SkullMeta) is.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add(Locale.SELECT_SKIN);
        sm.setDisplayName(name);
        sm.setLore(lore);
        is.setItemMeta(sm);
        is = setSkin(is, ((Property) s).getValue());
        return is;
    }

    private static ItemStack setSkin(ItemStack head, String b64stringtexture) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        PropertyMap propertyMap = profile.getProperties();
        if (propertyMap == null) {
            throw new IllegalStateException("Profile doesn't contain a property map");
        }
        propertyMap.put("textures", new Property("textures", b64stringtexture));
        ItemMeta headMeta = head.getItemMeta();
        Class<?> headMetaClass = headMeta.getClass();
        try {
            ReflectionUtil.getField(headMetaClass, "profile", GameProfile.class, 0).set(headMeta, profile);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
        head.setItemMeta(headMeta);
        return head;
    }

    public static ConcurrentHashMap<String, Integer> getMenus() {
        return openedMenus;
    }

    @EventHandler
    public void onCLick(InventoryClickEvent e) {
        if (!e.getInventory().getName().contains("Skins Menu")) {
            return;
        }
        Player player = (Player) e.getWhoClicked();
        if (e.getCurrentItem() != null) {
            if (!e.getCurrentItem().hasItemMeta()) {
                e.setCancelled(true);
                return;
            }
            if (e.getCurrentItem().getType() == Material.SKELETON_SKULL) {
                Object skin = SkinStorage.getSkinDataMenu(e.getCurrentItem().getItemMeta().getDisplayName());
                SkinStorage.setPlayerSkin(player.getName(), e.getCurrentItem().getItemMeta().getDisplayName());
                SkinsRestorer.getInstance().getFactory().applySkin(player, skin);
                SkinsRestorer.getInstance().getFactory().updateSkin(player);
                player.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
                player.closeInventory();
            } else if (e.getCurrentItem().getType() == Material.WHITE_STAINED_GLASS_PANE && e.getCurrentItem().getDurability() == 14) {
                if (SkinStorage.getPlayerSkin(player.getName()) == null) {
                    SkinStorage.removePlayerSkin(player.getName());
                    Object props = SkinStorage.createProperty("textures", "", "");
                    SkinsRestorer.getInstance().getFactory().applySkin(player, props);
                    SkinsRestorer.getInstance().getFactory().updateSkin(player);
                    player.closeInventory();
                    e.setCancelled(true);
                    return;
                }
                if (!Objects.requireNonNull(SkinStorage.getPlayerSkin(player.getName())).equalsIgnoreCase(player.getName())) {
                    SkinStorage.removePlayerSkin(player.getName());
                    Object props = SkinStorage.createProperty("textures", "", "");
                    SkinsRestorer.getInstance().getFactory().applySkin(player, props);
                    SkinsRestorer.getInstance().getFactory().updateSkin(player);
                    player.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
                    player.closeInventory();
                } else {
                    player.sendMessage(Locale.NO_SKIN_DATA);
                }
            } else if (e.getCurrentItem().getType() == Material.WHITE_STAINED_GLASS_PANE && e.getCurrentItem().getItemMeta().getDisplayName().contains("Next Page")) {
                int currentPage = getMenus().get(player.getName());
                getMenus().put(player.getName(), currentPage + 1);
                player.openInventory(getGUI(currentPage + 1));
            } else if (e.getCurrentItem().getType() == Material.WHITE_STAINED_GLASS_PANE && e.getCurrentItem().getItemMeta().getDisplayName().contains("Previous Page")) {
                int currentPage = getMenus().get(player.getName());
                getMenus().put(player.getName(), currentPage - 1);
                player.openInventory(getGUI(currentPage - 1));
            }
        }
        e.setCancelled(true);
    }
}