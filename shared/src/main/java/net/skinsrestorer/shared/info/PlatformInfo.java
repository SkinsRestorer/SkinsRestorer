package net.skinsrestorer.shared.info;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class PlatformInfo {
    private final String platformName;
    private final String platformVendor;
    private final String platformVersion;
    private final List<PluginInfo> plugins;
}
