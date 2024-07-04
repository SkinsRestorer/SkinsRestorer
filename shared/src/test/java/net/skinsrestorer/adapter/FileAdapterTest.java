/*
 * SkinsRestorer
 * Copyright (C) 2024  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.adapter;

import ch.jalu.configme.SettingsManager;
import ch.jalu.injector.Injector;
import net.skinsrestorer.SRExtension;
import net.skinsrestorer.SettingsHelper;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.storage.adapter.file.FileAdapter;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    public void setup() {
        SettingsHelper.returnDefaultsForAllProperties(settingsManager);
    }

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
