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
import skinsrestorer.shared.utils.ReflectionUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SkinsGUI extends ItemStack implements Listener {
    private SkinsRestorer plugin;
    private static ConcurrentHashMap<String, Integer> openedMenus = new ConcurrentHashMap<>();

    public SkinsGUI(SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    public static enum GlassType {
        NONE, PREV, NEXT, DELETE
    }

    public class GuiGlass {
        public GuiGlass(GlassType glassType) {
            this.glassType = glassType;
            this.create();
        }

        private void create() {
            switch (glassType) {
                case NONE: {
                    this.itemStack = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
                    this.text = "";
                    break;
                }

                case PREV: {
                    this.itemStack = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
                    this.text = Locale.PREVIOUS_PAGE;
                    break;
                }

                case NEXT: {
                    this.itemStack = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
                    this.text = Locale.NEXT_PAGE;
                    break;
                }

                case DELETE: {
                    this.itemStack = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                    this.text = Locale.REMOVE_SKIN;
                    break;
                }
            }

            this.itemMeta = this.itemStack.getItemMeta();
            this.itemMeta.setDisplayName(text);
            this.itemStack.setItemMeta(this.itemMeta);
        }

        private GlassType glassType;
        private ItemStack itemStack;
        private ItemMeta itemMeta;
        private String text;

        public ItemStack getItemStack() {
            return itemStack;
        }

        public String getText() {
            return text;
        }
    }

    public Inventory getGUI(Player p, int page) {
        Inventory inventory = Bukkit.createInventory(p, 54, "§9Skins Menu - Page " + page);
        int skinNumber = 36 * page;
        Map<String, Object> skinsList = plugin.getSkinStorage().getSkins(skinNumber);
        inventory.setItem(36, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(37, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(38, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(39, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(40, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(41, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(42, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(43, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(44, new GuiGlass(GlassType.NONE).getItemStack());

        //Middle button //remove skin
        inventory.setItem(48, new GuiGlass(GlassType.DELETE).getItemStack());
        inventory.setItem(49, new GuiGlass(GlassType.DELETE).getItemStack());
        inventory.setItem(50, new GuiGlass(GlassType.DELETE).getItemStack());
        //button place next
        inventory.setItem(45, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(46, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(47, new GuiGlass(GlassType.NONE).getItemStack());

        //button place next
        inventory.setItem(53, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(52, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(51, new GuiGlass(GlassType.NONE).getItemStack());

        for (String s : skinsList.keySet()) {
            if (skinsList.get(s) != null) {

                //if page is not 0, adding Previous Page button.
                if (page != 0) {
                    inventory.setItem(45, new GuiGlass(GlassType.PREV).getItemStack());
                    inventory.setItem(46, new GuiGlass(GlassType.PREV).getItemStack());
                    inventory.setItem(47, new GuiGlass(GlassType.PREV).getItemStack());
                }
                //if the page is full, adding Next Page button.
                if (inventory.firstEmpty() == -1) {
                    inventory.setItem(53, new GuiGlass(GlassType.NEXT).getItemStack());
                    inventory.setItem(52, new GuiGlass(GlassType.NEXT).getItemStack());
                    inventory.setItem(51, new GuiGlass(GlassType.NEXT).getItemStack());
                    continue;
                }
                inventory.addItem(createSkull(skinsList.get(s), s));
            }
        }
        return inventory;
    }

    private static ItemStack createSkull(Object s, String name) {
        ItemStack is = new ItemStack(Material.PLAYER_HEAD, 1, (short) 3);
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
        try {
            if (!e.getView().getTitle().contains("Skins Menu")) {
                return;
            }
        } catch (IllegalStateException ex) {
            return;
        }
        Player player = (Player) e.getWhoClicked();

        if (e.getCurrentItem() == null) {
            e.setCancelled(true);
            return;
        }

        if (!e.getCurrentItem().hasItemMeta()) {
            e.setCancelled(true);
            return;
        }

        // Todo use setSkin function from SkinCommand.class
        if (e.getCurrentItem().getType() == Material.PLAYER_HEAD) {
            Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
                Object skin = plugin.getSkinStorage().getSkinDataMenu(e.getCurrentItem().getItemMeta().getDisplayName());
                plugin.getSkinStorage().setPlayerSkin(player.getName(), e.getCurrentItem().getItemMeta().getDisplayName());
                SkinsRestorer.getInstance().getFactory().applySkin(player, skin);
                SkinsRestorer.getInstance().getFactory().updateSkin(player);
                player.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
            });
            player.closeInventory();
        } else if (e.getCurrentItem().getType() == Material.RED_STAINED_GLASS_PANE) {
            player.performCommand("skinsrestorer:skin clear");
            player.closeInventory();
        } else if (e.getCurrentItem().getType() == Material.GREEN_STAINED_GLASS_PANE && e.getCurrentItem().getItemMeta().getDisplayName().contains("Next Page")) {
            int currentPage = getMenus().get(player.getName());
            Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
                getMenus().put(player.getName(), currentPage + 1);
                Inventory newInventory = getGUI(((Player) e.getWhoClicked()).getPlayer(), currentPage + 1);

                Bukkit.getScheduler().scheduleSyncDelayedTask(SkinsRestorer.getInstance(), () -> {
                    player.openInventory(newInventory);
                });
            });
        } else if (e.getCurrentItem().getType() == Material.YELLOW_STAINED_GLASS_PANE && e.getCurrentItem().getItemMeta().getDisplayName().contains("Previous Page")) {
            int currentPage = getMenus().get(player.getName());
            Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
                getMenus().put(player.getName(), currentPage - 1);
                Inventory newInventory = getGUI(((Player) e.getWhoClicked()).getPlayer(), currentPage - 1);

                Bukkit.getScheduler().scheduleSyncDelayedTask(SkinsRestorer.getInstance(), () -> {
                    player.openInventory(newInventory);
                });
            });
        }

        e.setCancelled(true);

    }
}