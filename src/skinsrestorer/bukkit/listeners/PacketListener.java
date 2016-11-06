package skinsrestorer.bukkit.listeners;

import java.util.List;

import org.bukkit.entity.Player;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.ReflectionUtil;

public class PacketListener extends ChannelDuplexHandler {

	private Player p;
	private Class<?> PlayOutTileEntityData;
	private Class<?> PlayOutPlayerInfo;
	private Enum<?> ADD_PLAYER;

	public PacketListener(Player p) {
		this.p = p;
		try {
			PlayOutTileEntityData = ReflectionUtil.getNMSClass("PacketPlayOutTileEntityData");
			PlayOutPlayerInfo = ReflectionUtil.getNMSClass("PacketPlayOutPlayerInfo");
			ADD_PLAYER = ReflectionUtil.getEnum(PlayOutPlayerInfo, "EnumPlayerInfoAction", "ADD_PLAYER");
		} catch (Exception e) {
		}
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (PlayOutTileEntityData.isInstance(msg)) {

			Object tag = ReflectionUtil.getObject(msg, "c");
			Object owtag = ReflectionUtil.invokeMethod(tag.getClass(), tag, "getCompound",
					new Class<?>[] { String.class }, "Owner");

			if (owtag != null) {
				String owner = owtag.toString();

				if (owner.contains("\"\"") || (owner.contains("textures") && owner.contains("Signature:\"\"")))
					return;
			}

		} else if (PlayOutPlayerInfo.isInstance(msg)) {

			if (ADD_PLAYER == null) {
				int action = (int) ReflectionUtil.getObject(msg, "action");

				if (action == 0) {

					Object profile = ReflectionUtil.getObject(msg, "player");
					Object propmap = ReflectionUtil.getObject(profile, "properties");

					try {
						SkinsRestorer.getInstance().getFactory().applySkin(p,
								SkinStorage.getOrCreateSkinForPlayer(p.getName()), propmap);
					} catch (Exception e) {
					}

				}
			} else {

				Object action = ReflectionUtil.getObject(msg, "a");

				if (ADD_PLAYER.equals(action)) {
					List<?> playerInfos = (List<?>) ReflectionUtil.getObject(msg, "b");

					for (Object data : playerInfos) {
						Object profile = ReflectionUtil.getObject(data, "d");
						Object propmap = ReflectionUtil.getObject(profile, "properties");

						try {
							SkinsRestorer.getInstance().getFactory().applySkin(p,
									SkinStorage.getOrCreateSkinForPlayer(p.getName()), propmap);
						} catch (Exception e) {
						}
					}
				}
			}
		}

		super.write(ctx, msg, promise);
	}

	public static void inject(Player p) {
		try {
			Object craftHandle = ReflectionUtil.invokeMethod(p.getClass(), p, "getHandle");
			Object playerCon = ReflectionUtil.getField(craftHandle.getClass(), "playerConnection").get(craftHandle);
			Object manager = ReflectionUtil.getField(playerCon.getClass(), "networkManager").get(playerCon);
			Channel channel = (Channel) ReflectionUtil.getField(manager.getClass(), "channel").get(manager);

			if (channel.pipeline().context("PacketListener") != null)
				channel.pipeline().remove("PacketListener");

			channel.pipeline().addAfter("encoder", "PacketListener", new PacketListener(p));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void uninject(Player p) {
		try {
			Object craftHandle = ReflectionUtil.invokeMethod(p.getClass(), p, "getHandle");
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
