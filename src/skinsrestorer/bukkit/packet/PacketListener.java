package skinsrestorer.bukkit.packet;

import java.util.List;

import org.bukkit.entity.Player;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.ReflectionUtil;

public class PacketListener extends ChannelDuplexHandler {

	private Player p;
	private Class<?> PlayInChat;
	private Class<?> PlayOutTileEntityData;
	private Class<?> PlayOutPlayerInfo;
	private Enum<?> ADD_PLAYER;

	public PacketListener(Player p) {
		this.p = p;
		try {
			PlayOutTileEntityData = ReflectionUtil.getNMSClass("PacketPlayOutTileEntityData");
			PlayOutPlayerInfo = ReflectionUtil.getNMSClass("PacketPlayOutPlayerInfo");
			ADD_PLAYER = ReflectionUtil.getEnum(PlayOutPlayerInfo, "EnumPlayerInfoAction", "ADD_PLAYER");
			PlayInChat = ReflectionUtil.getNMSClass("PacketPlayInChat");
		} catch (Exception e) {
		}
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		// Skull crash fix part 1
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
			Object action = ReflectionUtil.getObject(msg, "a");

			if (ADD_PLAYER.equals(action)) {
				List<?> playerInfos = (List<?>) ReflectionUtil.getObject(msg, "b");

				for (Object data : playerInfos) {
					Object profile = ReflectionUtil.getObject(data, "d");
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

		}

		super.write(ctx, msg, promise);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (PlayInChat.isInstance(msg)) {
			try {
				String cmd = (String) ReflectionUtil.getObject(msg, "a");
				if (cmd.startsWith("/skinver")) {
					p.sendMessage(C.c("&8This server is kindly running &aSkinsRestorer &e"
							+ SkinsRestorer.getInstance().getVersion() + "&8, made with love by &d"
							+ SkinsRestorer.getInstance().getDescription().getAuthors().get(0) + " &8and &c"
							+ SkinsRestorer.getInstance().getDescription().getAuthors().get(1)
							+ "&8, utilizing Minecraft &a" + ReflectionUtil.serverVersion + "&8."));
					return;
				}
			} catch (Exception e) {
			}
		}
		super.channelRead(ctx, msg);
	}

	public static void inject(Player p) {
		try {
			Object craftHandle = ReflectionUtil.invokeMethod(p.getClass(), p, "getHandle");
			Object playerCon = ReflectionUtil.getField(craftHandle.getClass(), "playerConnection").get(craftHandle);
			Object manager = ReflectionUtil.getField(playerCon.getClass(), "networkManager").get(playerCon);
			Channel channel = (Channel) ReflectionUtil.getFirstObject(manager.getClass(), Channel.class, manager);

			if (channel.pipeline().context("SkinsRestorer-Listener") != null)
				channel.pipeline().remove("SkinsRestorer-Listener");

			channel.pipeline().addBefore("packet_handler", "SkinsRestorer-Listener", new PacketListener(p));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void uninject(Player p) {
		try {
			Object craftHandle = ReflectionUtil.invokeMethod(p.getClass(), p, "getHandle");
			Object playerCon = ReflectionUtil.getField(craftHandle.getClass(), "playerConnection").get(craftHandle);
			Object manager = ReflectionUtil.getField(playerCon.getClass(), "networkManager").get(playerCon);
			Channel channel = (Channel) ReflectionUtil.getFirstObject(manager.getClass(), Channel.class, manager);

			if (channel.pipeline().context("SkinsRestorer-Listener") != null)
				channel.pipeline().remove("SkinsRestorer-Listener");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
