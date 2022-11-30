import net.skinsrestorer.bukkit.SkinsRestorerBukkit;
import net.skinsrestorer.shared.exception.InitializeException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.*;
import org.bukkit.help.HelpMap;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

import static org.mockito.Mockito.*;

public class LoadTest {
    @Test
    public void testLoad() throws InitializeException {
        System.setProperty("nms.version", "1_19_R2");
        ServerMock server = mock(ServerMock.class);
        Logger logger = Logger.getLogger("TestSkinsRestorer");
        ConsoleCommandSender sender =  mock(ConsoleCommandSender.class);
        doAnswer(invocation -> {
            Object arg0 = invocation.getArgument(0);
            logger.info(String.valueOf(arg0));
            return null;
        }).when(sender).sendMessage(anyString());

        Path test = Paths.get("test");
        when(server.getLogger()).thenReturn(logger);
        when(server.getConsoleSender()).thenReturn(sender);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        when(server.getScheduler()).thenReturn(mock(BukkitScheduler.class));
        when(server.getServicesManager()).thenReturn(mock(ServicesManager.class));
        when(server.getPluginCommand(anyString())).thenReturn(mock(PluginCommand.class));
        when(server.getUpdateFolderFile()).thenReturn(test.toFile());
        when(server.getUpdateFolder()).thenReturn("test");
        when(server.getBukkitVersion()).thenReturn("1.19.2-R0.1-SNAPSHOT");
        when(server.getName()).thenReturn("TestServer");
        when(server.getCommandMap()).thenReturn(mock(SimpleCommandMap.class));

        try (MockedStatic<Bukkit> ignored = mockStatic(Bukkit.class)) {
            when(Bukkit.getVersion()).thenReturn("1.19.2-R0.1-SNAPSHOT");
            when(Bukkit.getServer()).thenReturn(server);
            when(Bukkit.getHelpMap()).thenReturn(mock(HelpMap.class));
            when(Bukkit.getPluginManager()).thenReturn(mock(PluginManager.class));
            when(Bukkit.getScheduler()).thenReturn(mock(BukkitScheduler.class));

            JavaPlugin plugin = mock(JavaPlugin.class);
            when(plugin.getDataFolder()).thenReturn(test.toFile());
            when(plugin.getServer()).thenReturn(server);
            when(plugin.getLogger()).thenReturn(logger);
            when(plugin.getName()).thenReturn("SkinsRestorer");
            when(plugin.getDescription()).thenReturn(mock(PluginDescriptionFile.class));
            when(plugin.isEnabled()).thenReturn(true);
            when(plugin.getPluginLoader()).thenReturn(mock(JavaPluginLoader.class));

            new SkinsRestorerBukkit(server, "1.0.0", Paths.get("testrun"), plugin, true).pluginStartup();
        }
    }

    public abstract static class ServerMock implements Server {
        abstract CommandMap getCommandMap();
    }
}
