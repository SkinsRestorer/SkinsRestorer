package net.skinsrestorer.shared.info;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PluginInfo {
    private final boolean enabled;
    private final String name;
    private final String version;
    private final String entryPoint;
    private final String[] authors;
}
