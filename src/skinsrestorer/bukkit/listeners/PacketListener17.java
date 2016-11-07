package skinsrestorer.bukkit.listeners;

import org.bukkit.entity.Player;

import net.minecraft.util.io.netty.channel.Channel;
import net.minecraft.util.io.netty.channel.ChannelDuplexHandler;
import net.minecraft.util.io.netty.channel.ChannelHandlerContext;
import net.minecraft.util.io.netty.channel.ChannelPromise;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.ReflectionUtil;

public class PacketListener17 extends ChannelDuplexHandler {

	private Player p;
	private Class<?> PlayOutTileEntityData;
	private Class<?> PlayOutPlayerInfo;

	public PacketListener17(Player p) {
		this.p = p;
		try {
			PlayOutTileEntityData = ReflectionUtil.getNMSClass("PacketPlayOutTileEntityData");
			PlayOutPlayerInfo = ReflectionUtil.getNMSClass("PacketPlayOutPlayerInfo");
		} catch (Exception e) {
		}
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		try {
			if (PlayOutTileEntityData.isInstance(msg)) {

				Object tag = ReflectionUtil.getObject(msg, "e");
				Object owtag = ReflectionUtil.invokeMethod(tag.getClass(), tag, "getCompound",
						new Class<?>[] { String.class }, "Owner");

				if (owtag != null) {
					String owner = owtag.toString();

					if (owner.contains("\"\"") || (owner.contains("textures") && owner.contains("Signature:\"\"")))
						return;
				}

			} else if (PlayOutPlayerInfo.isInstance(msg)) {
				int action = (int) ReflectionUtil.getObject(msg, "action");

				if (action == 0) {

					Object profile = ReflectionUtil.getObject(msg, "player");
					// UUID id = (UUID) ReflectionUtil.getObject(profile, "id");
					String name = (String) ReflectionUtil.getObject(profile, "name");
					Object propmap = ReflectionUtil.getObject(profile, "properties");

					if (p.getName().equals(name)) {
						try {
							SkinsRestorer.getInstance().getFactory().applySkin(p,
									SkinStorage.getOrCreateSkinForPlayer(p.getName()), propmap);
						} catch (Exception e) {
						}
					} else {

						try {
							String skin = SkinStorage.getPlayerSkin(name);
							if (skin == null)
								skin = name;

							SkinsRestorer.getInstance().getFactory().applySkin(p, SkinStorage.getSkinData(skin),
									propmap);
						} catch (Exception e) {
						}
					}

				}
			}

			super.write(ctx, msg, promise);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void inject(Player p) {
		try {
			Object craftOnline = ReflectionUtil.getBukkitClass("entity.CraftPlayer").cast(p);
			Object craftHandle = ReflectionUtil.invokeMethod(craftOnline.getClass(), craftOnline, "getHandle");
			Object playerCon = ReflectionUtil.getField(craftHandle.getClass(), "playerConnection").get(craftHandle);
			Object manager = ReflectionUtil.getField(playerCon.getClass(), "networkManager").get(playerCon);
			Channel channel = (Channel) ReflectionUtil.getFirstObject(manager.getClass(), Channel.class, manager);

			if (channel.pipeline().context("PacketListener17") != null)
				channel.pipeline().remove("PacketListener17");

			channel.pipeline().addAfter("encoder", "PacketListener17", new PacketListener17(p));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void uninject(Player p) {
		try {
			Object craftOnline = ReflectionUtil.getBukkitClass("entity.CraftPlayer").cast(p);
			Object craftHandle = ReflectionUtil.invokeMethod(craftOnline.getClass(), craftOnline, "getHandle");
			Object playerCon = ReflectionUtil.getField(craftHandle.getClass(), "playerConnection").get(craftHandle);
			Object manager = ReflectionUtil.getField(playerCon.getClass(), "networkManager").get(playerCon);
			Channel channel = (Channel) ReflectionUtil.getFirstObject(manager.getClass(), Channel.class, manager);

			if (channel.pipeline().context("PacketListener17") != null)
				channel.pipeline().remove("PacketListener17");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
