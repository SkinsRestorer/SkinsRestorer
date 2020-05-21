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
import skinsrestorer.shared.storage.Config;
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
        Inventory inventory = Bukkit.createInventory(p, 54, Locale.SKINSMENU_TITLE.replace("&", "§") + page);

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

        skinsList.forEach((name, property) -> {
            //if page is not 0, adding Previous Page button.
            if (page != 0) {
                inventory.setItem(45, new GuiGlass(GlassType.PREV).getItemStack());
                inventory.setItem(46, new GuiGlass(GlassType.PREV).getItemStack());
                inventory.setItem(47, new GuiGlass(GlassType.PREV).getItemStack());
            }

            inventory.addItem(createSkull(name, property));

            //if the page is full, adding Next Page button.
            if (inventory.firstEmpty() == -1 || inventory.getItem(26) != null) {
                inventory.setItem(53, new GuiGlass(GlassType.NEXT).getItemStack());
                inventory.setItem(52, new GuiGlass(GlassType.NEXT).getItemStack());
                inventory.setItem(51, new GuiGlass(GlassType.NEXT).getItemStack());
                return;
            }
        });

        return inventory;
    }

    public Inventory getGUI(Player p, int page) {
        int skinNumber = 36 * page;
        Map<String, Object> skinsList = plugin.getSkinStorage().getSkins(skinNumber);
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
        setSkin(is, ((Property) property).getValue());
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

    @EventHandler
    public void onCLick(InventoryClickEvent e) {
        try {
            if (!e.getView().getTitle().contains("Skins Menu") && !e.getView().getTitle().contains(Locale.SKINSMENU_TITLE)) {
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

        if (plugin.isBungeeEnabled()) {
            if (XMaterial.matchXMaterial(e.getCurrentItem()) == XMaterial.PLAYER_HEAD) {
                String skin = e.getCurrentItem().getItemMeta().getDisplayName();
                plugin.requestSkinSetFromBungeeCord(player, skin);
                player.closeInventory();
            }
            if (XMaterial.matchXMaterial(e.getCurrentItem()) == XMaterial.RED_STAINED_GLASS_PANE) {
                plugin.requestSkinClearFromBungeeCord(player);
                player.closeInventory();
            }
            if (XMaterial.matchXMaterial(e.getCurrentItem()) == XMaterial.GREEN_STAINED_GLASS_PANE) {
                int currentPage = getMenus().get(player.getName());
                getMenus().put(player.getName(), currentPage + 1);
                plugin.requestSkinsFromBungeeCord(player, currentPage + 1);
            }
            if (XMaterial.matchXMaterial(e.getCurrentItem()) == XMaterial.YELLOW_STAINED_GLASS_PANE) {
                int currentPage = getMenus().get(player.getName());
                getMenus().put(player.getName(), currentPage - 1);
                plugin.requestSkinsFromBungeeCord(player, currentPage - 1);
            }
            e.setCancelled(true);
            return;
        }

        // Todo use setSkin function from SkinCommand.class
        if (XMaterial.matchXMaterial(e.getCurrentItem()) == XMaterial.PLAYER_HEAD) {
            Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
                Object skin = plugin.getSkinStorage().getSkinData(e.getCurrentItem().getItemMeta().getDisplayName(), false);

                // PerSkinPermissions //todo: should be moved to setskin() as a command so it includes both cooldown and already used code from below
                if (Config.PER_SKIN_PERMISSIONS && Config.USE_NEW_PERMISSIONS) {
                    String skinname = e.getCurrentItem().getItemMeta().getDisplayName();
                    if (!player.hasPermission("skinsrestorer.skin." + skinname)) {
                        if (!player.getName().equals(skinname) || (!player.hasPermission("skinsrestorer.ownskin") && !skinname.equalsIgnoreCase(player.getName()))) {
                            player.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION_SKIN);
                            return;
                        }
                    }
                }

                plugin.getSkinStorage().setPlayerSkin(player.getName(), e.getCurrentItem().getItemMeta().getDisplayName());
                SkinsRestorer.getInstance().getFactory().applySkin(player, skin);
                SkinsRestorer.getInstance().getFactory().updateSkin(player);
                player.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
            });
            player.closeInventory();
        } else if (XMaterial.matchXMaterial(e.getCurrentItem()) == XMaterial.RED_STAINED_GLASS_PANE) {
            player.performCommand("skinsrestorer:skin clear");
            player.closeInventory();
        } else if (XMaterial.matchXMaterial(e.getCurrentItem()) == XMaterial.GREEN_STAINED_GLASS_PANE) {
            int currentPage = getMenus().get(player.getName());
            Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
                getMenus().put(player.getName(), currentPage + 1);
                Inventory newInventory = getGUI(((Player) e.getWhoClicked()).getPlayer(), currentPage + 1);

                Bukkit.getScheduler().scheduleSyncDelayedTask(SkinsRestorer.getInstance(), () -> {
                    player.openInventory(newInventory);
                });
            });
        } else if (XMaterial.matchXMaterial(e.getCurrentItem()) == XMaterial.YELLOW_STAINED_GLASS_PANE) {
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