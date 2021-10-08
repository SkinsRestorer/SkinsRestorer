package net.skinsrestorer.api.interfaces;

import net.skinsrestorer.api.property.IProperty;

import java.util.Optional;

public interface IMojangAPI {
    Optional<IProperty> getProfile(String uuid);
}
