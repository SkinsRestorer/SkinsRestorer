package net.skinsrestorer.adapter;

import ch.jalu.configme.SettingsManager;
import ch.jalu.injector.Injector;
import net.skinsrestorer.SRExtension;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.storage.adapter.file.FileAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, SRExtension.class})
public class FileAdapterTest {
    @TempDir
    private Path tempDir;
    @Mock
    private SettingsManager settingsManager;

    @Test
    public void testLoad(Injector injector) {
        injector.register(SettingsManager.class, settingsManager);
        SRPlugin plugin = mock(SRPlugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir);
        injector.register(SRPlugin.class, plugin);

        FileAdapter adapter = injector.getSingleton(FileAdapter.class);
        adapter.init();

        AdapterHelper.testAdapter(adapter);
    }
}
