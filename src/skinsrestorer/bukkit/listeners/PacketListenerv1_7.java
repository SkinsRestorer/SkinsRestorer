package skinsrestorer.bukkit.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.minecraft.util.io.netty.channel.Channel;
import net.minecraft.util.io.netty.channel.ChannelHandlerContext;
import net.minecraft.util.io.netty.channel.ChannelPromise;
import net.minecraft.util.io.netty.channel.ChannelDuplexHandler;
import skinsrestorer.shared.utils.ReflectionUtil;

public class PacketListenerv1_7 extends ChannelDuplexHandler {

	@SuppressWarnings("unused")
	private Player p;
	private Class<?> PlayOutTileEntityData;
	private Class<?> PlayOutPlayerInfo;

	public PacketListenerv1_7(Player p) {
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
			// Todo check for skin validity?
			// Skins should never get sent with null property
			// It never happens as I made it like that
			// But you can never know amirite
		}

		super.write(ctx, msg, promise);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void injectForAll() {
		for (Player p : Bukkit.getOnlinePlayers())
			inject(p);
	}

	public static void uninjectForAll() {
		for (Player p : Bukkit.getOnlinePlayers())
			uninject(p);
	}

	public static void inject(Player p) {
		try {
			Object craftOnline = ReflectionUtil.getBukkitClass("entity.CraftPlayer").cast(p);
			Object craftHandle = ReflectionUtil.invokeMethod(craftOnline.getClass(), craftOnline, "getHandle");
			Object playerCon = ReflectionUtil.getField(craftHandle.getClass(), "playerConnection").get(craftHandle);
			Object manager = ReflectionUtil.getField(playerCon.getClass(), "networkManager").get(playerCon);
			Channel channel = (Channel) ReflectionUtil.getField(manager.getClass(), "m").get(manager);

			if (channel.pipeline().context("PacketListenerv1_7") != null)
				channel.pipeline().remove("PacketListenerv1_7");
			// channel.pipeline().addBefore("packet_handler", "PacketListener",
			// new PacketHandler(p));
			channel.pipeline().addAfter("encoder", "PacketListenerv1_7", new PacketListenerv1_7(p));
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
			Channel channel = (Channel) ReflectionUtil.getField(manager.getClass(), "m").get(manager);

			if (channel.pipeline().context("PacketListenerv1_7") != null)
				channel.pipeline().remove("PacketListenerv1_7");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
