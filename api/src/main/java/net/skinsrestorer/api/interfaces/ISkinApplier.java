package net.skinsrestorer.api.interfaces;

import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.property.IProperty;

public interface ISkinApplier {
    void applySkin(PlayerWrapper playerWrapper, IProperty property);
}
