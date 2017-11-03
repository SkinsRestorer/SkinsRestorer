package skinsrestorer.sponge;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

import com.google.inject.Inject;

import org.bstats.sponge.Metrics;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import skinsrestorer.sponge.commands.SkinCommand;
import skinsrestorer.sponge.listeners.LoginListener;

@Plugin(id = "skinsrestorer", name = "SkinsRestorer", version = "1.0", 
	description = "Restore & Change your skin!", 
	authors = "Th3Tr0LLeR, Blackfire62")

public class SkinsRestorer {
	
	@SuppressWarnings("unused")
	@Inject
	private Metrics metrics;

	private static SkinsRestorer instance;

	public static SkinsRestorer getInstance() {
		return instance;
	}

	private String directory;
	private ConfigurationLoader<CommentedConfigurationNode> configManager;

	private ConfigurationLoader<CommentedConfigurationNode> dataManager;
	private CommentedConfigurationNode configRoot;

	private CommentedConfigurationNode dataRoot;

	public CommentedConfigurationNode getConfigRoot() {
		return configRoot;
	}

	public CommentedConfigurationNode getDataRoot() {
		return dataRoot;
	}

	@Listener
	public void onInitialize(GameInitializationEvent e) {
		instance = this;

		directory = Sponge.getGame().getConfigManager().getPluginConfig(this).getDirectory().toString();

		try {
			reloadConfigs();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		if (!Sponge.getServer().getOnlineMode())
			Sponge.getEventManager().registerListener(this, ClientConnectionEvent.Login.class, new LoginListener());

		CommandSpec skinCommand = CommandSpec.builder().description(Text.of("Set your skin"))
				.arguments(GenericArguments.onlyOne(GenericArguments.string(Text.of("skin"))))
				.executor(new SkinCommand()).build();

		Sponge.getCommandManager().register(this, skinCommand, "skin");
	}

	public void reloadConfigs() {
		try {
			File dir = new File(directory);

			if (!dir.exists())
				dir.mkdirs();

			File config = new File(directory + File.separator + "config.conf");
			File data = new File(directory + File.separator + "data.conf");

			if (config.exists()) {
				config.createNewFile();
				try {
					Files.copy(this.getClass().getResource("config.conf").openStream(),
							config.getAbsoluteFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (!data.exists())
				data.createNewFile();

			configManager = HoconConfigurationLoader.builder().setPath(config.toPath()).build();
			dataManager = HoconConfigurationLoader.builder().setPath(data.toPath()).build();

			configRoot = configManager.load();
			dataRoot = dataManager.load();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveConfigs() {
		try {
			configManager.save(configRoot);
			dataManager.save(dataRoot);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
