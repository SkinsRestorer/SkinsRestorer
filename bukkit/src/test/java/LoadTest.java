import net.skinsrestorer.bukkit.SkinsRestorerBukkit;
import net.skinsrestorer.shared.exception.InitializeException;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandMap;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.help.HelpMap;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import static org.mockito.Mockito.*;

public class LoadTest {
    @BeforeAll
    public static void setup() {
        System.setProperty("sr.unit.test", "true");
    }

    @Test
    public void testLoad() throws InitializeException {
        UUID runId = UUID.randomUUID();
        Path baseDir = Paths.get("build/testrun/" + runId);
        System.out.println("Running test with runId " + runId);
        Path configDir = baseDir.resolve("config");

        Queue<Runnable> runQueue = new ConcurrentLinkedQueue<>();
        System.setProperty("nms.version", "1_19_R2");
        ServerMock server = mock(ServerMock.class);
        Logger logger = Logger.getLogger("TestSkinsRestorer");
        ConsoleCommandSender sender = mock(ConsoleCommandSender.class);
        doAnswer(invocation -> {
            Object arg0 = invocation.getArgument(0);
            System.out.println(arg0);
            return null;
        }).when(sender).sendMessage(anyString());

        when(server.getLogger()).thenReturn(logger);
        when(server.getConsoleSender()).thenReturn(sender);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        BukkitScheduler scheduler = mock(BukkitScheduler.class);

        doAnswer(invocation -> {
            runQueue.add(invocation.getArgument(1));
            return null;
        }).when(scheduler).runTaskAsynchronously(any(), any(Runnable.class));

        doAnswer(invocation -> {
            runQueue.add(invocation.getArgument(1));
            return null;
        }).when(scheduler).runTask(any(), any(Runnable.class));

        when(server.getScheduler()).thenReturn(scheduler);
        when(server.getServicesManager()).thenReturn(mock(ServicesManager.class));
        when(server.getPluginCommand(anyString())).thenReturn(mock(PluginCommand.class));
        when(server.getUpdateFolderFile()).thenReturn(baseDir.resolve("update").toFile());
        when(server.getUpdateFolder()).thenReturn("test");
        when(server.getBukkitVersion()).thenReturn("1.19.2-R0.1-SNAPSHOT");
        when(server.getName()).thenReturn("TestServer");
        when(server.getCommandMap()).thenReturn(mock(SimpleCommandMap.class));

        try (MockedStatic<Bukkit> ignored = mockStatic(Bukkit.class)) {
            when(Bukkit.getVersion()).thenReturn("1.19.2-R0.1-SNAPSHOT");
            when(Bukkit.getServer()).thenReturn(server);
            when(Bukkit.getHelpMap()).thenReturn(mock(HelpMap.class));
            when(Bukkit.getPluginManager()).thenReturn(mock(PluginManager.class));
            when(Bukkit.getScheduler()).thenReturn(scheduler);

            JavaPluginMock plugin = mock(JavaPluginMock.class);
            when(plugin.getDataFolder()).thenReturn(configDir.toFile());
            when(plugin.getServer()).thenReturn(server);
            when(plugin.getLogger()).thenReturn(logger);
            when(plugin.getName()).thenReturn("SkinsRestorer");
            when(plugin.getDescription()).thenReturn(mock(PluginDescriptionFile.class));
            when(plugin.isEnabled()).thenReturn(true);
            when(plugin.getPluginLoader()).thenReturn(mock(JavaPluginLoader.class));
            when(plugin.getFile()).thenReturn(null);

            new SkinsRestorerBukkit(server, "UnitTest", configDir, plugin, true).pluginStartup();

            while (!runQueue.isEmpty()) {
                try {
                    runQueue.poll().run();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        }
    }

    public abstract static class ServerMock implements Server {
        abstract CommandMap getCommandMap();
    }

    public abstract static class JavaPluginMock extends JavaPlugin {
        public abstract File getFile();
    }
}
