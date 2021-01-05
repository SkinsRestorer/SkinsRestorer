package skinsrestorer.bukkit;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.utils.ReflectionUtil;
import skinsrestorer.shared.utils.SRLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SkinsGUI extends ItemStack implements Listener {
    private static ConcurrentHashMap<String, Integer> openedMenus = new ConcurrentHashMap<>();
    @Getter
    private SRLogger srLogger;
    private SkinsRestorer plugin;
    private CommandSender console;

    public SkinsGUI(SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    public static enum GlassType {
        NONE, PREV, NEXT, DELETE
    }

    public static class GuiGlass {
        public GuiGlass(GlassType glassType) {
            this.glassType = glassType;
            this.create();
        }

        private void create() {
            switch (glassType) {
                case NONE: {
                    //this.itemStack = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
                    this.itemStack = XMaterial.WHITE_STAINED_GLASS_PANE.parseItem();

                    this.text = " ";
                    break;
                }

                case PREV: {
                    //this.itemStack = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
                    this.itemStack = XMaterial.YELLOW_STAINED_GLASS_PANE.parseItem();
                    this.text = Locale.SKINSMENU_PREVIOUS_PAGE.replace("&", "§");
                    break;
                }

                case NEXT: {
                    //this.itemStack = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
                    this.itemStack = XMaterial.GREEN_STAINED_GLASS_PANE.parseItem();
                    this.text = Locale.SKINSMENU_NEXT_PAGE.replace("&", "§");
                    break;
                }

                case DELETE: {
                    //this.itemStack = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                    this.itemStack = XMaterial.RED_STAINED_GLASS_PANE.parseItem();
                    this.text = Locale.SKINSMENU_REMOVE_SKIN.replace("&", "§");
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

    public Inventory getGUI(Player p, int page, Map<String, Object> skinsList) {
        Inventory inventory = Bukkit.createInventory(p, 54, Locale.SKINSMENU_TITLE_NEW.replace("&", "§").replace("%page", ""+page));

        //White Glass line
        inventory.setItem(36, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(37, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(38, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(39, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(40, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(41, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(42, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(43, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(44, new GuiGlass(GlassType.NONE).getItemStack());

        //empty place previous
        inventory.setItem(45, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(46, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(47, new GuiGlass(GlassType.NONE).getItemStack());

        //Middle button //remove skin
        inventory.setItem(48, new GuiGlass(GlassType.DELETE).getItemStack());
        inventory.setItem(49, new GuiGlass(GlassType.DELETE).getItemStack());
        inventory.setItem(50, new GuiGlass(GlassType.DELETE).getItemStack());


        //empty place next
        inventory.setItem(53, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(52, new GuiGlass(GlassType.NONE).getItemStack());
        inventory.setItem(51, new GuiGlass(GlassType.NONE).getItemStack());

        //if page is above 1, adding Previous Page button.
        if (page > 1) {
            inventory.setItem(45, new GuiGlass(GlassType.PREV).getItemStack());
            inventory.setItem(46, new GuiGlass(GlassType.PREV).getItemStack());
            inventory.setItem(47, new GuiGlass(GlassType.PREV).getItemStack());
        }

        skinsList.forEach((name, property) -> {
            if (!name.equals(name.toLowerCase())) {
                this.srLogger.logAlways("[SkinsRestorer] ERROR: skin " + name + ".skin contains a Upper case! \nPlease rename the file name to a lower case!.");
                return;
            }

            inventory.addItem(createSkull(name, property));

        });

        //if the page is not empty, adding Next Page button.
        //
        if (inventory.firstEmpty() == -1 || inventory.getItem(26) != null && page < 999) {
            inventory.setItem(53, new GuiGlass(GlassType.NEXT).getItemStack());
            inventory.setItem(52, new GuiGlass(GlassType.NEXT).getItemStack());
            inventory.setItem(51, new GuiGlass(GlassType.NEXT).getItemStack());
        }
        return inventory;
    }

    public Inventory getGUI(Player p, int page) {
        if (page > 999)
            page = 999;
        int skinNumber = 36 * page;
        Map<String, Object> skinsList = plugin.getSkinStorage().getSkins(skinNumber);
        ++page; // start counting from 1
        return this.getGUI(p, page, skinsList);
    }

    private ItemStack createSkull(String name, Object property) {
        ItemStack is = XMaterial.PLAYER_HEAD.parseItem();
        SkullMeta sm = (SkullMeta) is.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add(Locale.SKINSMENU_SELECT_SKIN.replace("&", "§"));
        sm.setDisplayName(name);
        sm.setLore(lore);
        is.setItemMeta(sm);
        try {
            setSkin(is, ((Property) property).getValue());
        } catch (Exception e){
            this.srLogger.logAlways("[SkinsRestorer] ERROR: could not add '" + name + "' to SkinsGUI, skin might be corrupted or invalid!");
            this.srLogger.log("[SkinsRestorer] DEBUG= " + e);        }
        return is;
    }

    private void setSkin(ItemStack head, String b64stringtexture) {
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
    }

    public static ConcurrentHashMap<String, Integer> getMenus() {
        return openedMenus;
    }

    //Todo increase performance by looking for instance of player and exclude non item clicks from this event before e.getView (use  if performance will be increased.)
    @EventHandler (ignoreCancelled = true)
    public void onCLick(InventoryClickEvent e) {
        //Cancel if clicked outside inventory
        if (e.getClickedInventory() == null) {
            return;
        }

        if (e.getView().getTopInventory().getType() != InventoryType.CHEST) {
            return;
        }

        try {
            if (!e.getView().getTitle().contains("Skins Menu") && !e.getView().getTitle().contains(Locale.SKINSMENU_TITLE_NEW.replace("%page", ""))) {
                return;
            }
        } catch (IllegalStateException ex) {
            return;
        }

        Player player = (Player) e.getWhoClicked();

        //Cancel picking up items
        if (e.getCurrentItem() == null) {
            e.setCancelled(true);
            return;
        }

        ItemStack currentItem = e.getCurrentItem();

        //Cancel white panels.
        if (!currentItem.hasItemMeta()) {
            e.setCancelled(true);
            return;
        }

        if (plugin.isBungeeEnabled()) {
            switch (XMaterial.matchXMaterial(currentItem)) {
                case PLAYER_HEAD:
                    String skin = currentItem.getItemMeta().getDisplayName();
                    plugin.requestSkinSetFromBungeeCord(player, skin);
                    player.closeInventory();
                    break;
                case RED_STAINED_GLASS_PANE:
                    plugin.requestSkinClearFromBungeeCord(player);
                    player.closeInventory();
                    break;
                case GREEN_STAINED_GLASS_PANE: {
                    int currentPage = getMenus().get(player.getName());
                    getMenus().put(player.getName(), currentPage + 1);
                    plugin.requestSkinsFromBungeeCord(player, currentPage + 1);
                    break;
                }
                case YELLOW_STAINED_GLASS_PANE: {
                    int currentPage = getMenus().get(player.getName());
                    getMenus().put(player.getName(), currentPage - 1);
                    plugin.requestSkinsFromBungeeCord(player, currentPage - 1);
                    break;
                }
            }
            e.setCancelled(true);
            return;
        }

        // Todo use setSkin function from SkinCommand.class
        switch (Objects.requireNonNull(XMaterial.matchXMaterial(currentItem))) {
            case PLAYER_HEAD:
                Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
                    Object skin = plugin.getSkinStorage().getSkinData(currentItem.getItemMeta().getDisplayName(), false);

                    // PerSkinPermissions //todo: should be moved to setskin() as a command so it includes both cooldown and already used code from below
                    if (Config.PER_SKIN_PERMISSIONS) {
                        String skinname = currentItem.getItemMeta().getDisplayName();
                        if (!player.hasPermission("skinsrestorer.skin." + skinname)) {
                            if (!player.getName().equals(skinname) || (!player.hasPermission("skinsrestorer.ownskin") && !skinname.equalsIgnoreCase(player.getName()))) {
                                player.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION_SKIN);
                                return;
                            }
                        }
                    }

                    plugin.getSkinStorage().setPlayerSkin(player.getName(), currentItem.getItemMeta().getDisplayName());
                    SkinsRestorer.getInstance().getFactory().applySkin(player, skin);
                    SkinsRestorer.getInstance().getFactory().updateSkin(player);
                    player.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
                });
                player.closeInventory();
                break;
            case RED_STAINED_GLASS_PANE:
                player.performCommand("skinsrestorer:skin clear");
                player.closeInventory();
                break;
            case GREEN_STAINED_GLASS_PANE: {
                int currentPage = getMenus().get(player.getName());
                Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
                    getMenus().put(player.getName(), currentPage + 1);
                    Inventory newInventory = getGUI((player).getPlayer(), currentPage + 1);

                    Bukkit.getScheduler().scheduleSyncDelayedTask(SkinsRestorer.getInstance(), () -> {
                        player.openInventory(newInventory);
                    });
                });
                break;
            }
            case YELLOW_STAINED_GLASS_PANE: {
                int currentPage = getMenus().get(player.getName());
                Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
                    getMenus().put(player.getName(), currentPage - 1);
                    Inventory newInventory = getGUI((player).getPlayer(), currentPage - 1);

                    Bukkit.getScheduler().scheduleSyncDelayedTask(SkinsRestorer.getInstance(), () -> {
                        player.openInventory(newInventory);
                    });
                });
                break;
            }
        }

        e.setCancelled(true);
    }
}
