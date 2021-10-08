package net.skinsrestorer.api.interfaces;

import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.IProperty;

import java.util.Optional;

public interface ISkinStorage {
    Optional<String> getSkinName(String playerName);

    void setSkinName(String playerName, String skinName);

    Optional<IProperty> getSkinData(String skinName);

    IProperty getSkinForPlayer(String skinName) throws SkinRequestException;

    void removeSkin(String playerName);
}
