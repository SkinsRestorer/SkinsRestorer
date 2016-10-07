package skinsrestorer.bukkit.listeners;

import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import skinsrestorer.shared.utils.ReflectionUtil;

public class PacketListener extends ChannelDuplexHandler {

	@SuppressWarnings("unused")
	private Player p;
	private Class<?> PlayOutTileEntityData;

	public PacketListener(Player p) {
		this.p = p;
		try {
			PlayOutTileEntityData = ReflectionUtil.getNMSClass("PacketPlayOutTileEntityData");
		} catch (Exception e) {
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (PlayOutTileEntityData.isInstance(msg)) {

			Object tag = ReflectionUtil.getObject(msg, "c");
			Object owner = ReflectionUtil.invokeMethod(tag.getClass(), tag, "getCompound",
					new Class<?>[] { String.class }, "Owner");

			if (!owner.toString().isEmpty()) {

				Object props = ReflectionUtil.invokeMethod(owner.getClass(), owner, "getCompound",
						new Class<?>[] { String.class }, "Properties");

				if (!props.toString().isEmpty()) {

					Map<String, Object> map = (Map<String, Object>) ReflectionUtil.getObject(props, "map");

					for (Entry<String, Object> entry : map.entrySet()) {

						if (!entry.getKey().equals("textures"))
							continue;

						String value = entry.getValue().toString();
						if (value.contains("\"\"") || !value.contains("Value:\"")) {
							return;
						}
					}

				}
			}

		}

		super.write(ctx, msg, promise);
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
			Channel channel = (Channel) ReflectionUtil.getField(manager.getClass(), "channel").get(manager);

			if (channel.pipeline().context("PacketListener") != null)
				channel.pipeline().remove("PacketListener");
			// channel.pipeline().addBefore("packet_handler", "PacketListener",
			// new PacketHandler(p));
			channel.pipeline().addAfter("encoder", "PacketListener", new PacketListener(p));
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
			Channel channel = (Channel) ReflectionUtil.getField(manager.getClass(), "channel").get(manager);

			if (channel.pipeline().context("PacketListener") != null)
				channel.pipeline().remove("PacketListener");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
