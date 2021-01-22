package net.skinsrestorer.shared.interfaces;

import net.skinsrestorer.shared.exception.SkinRequestException;
import net.skinsrestorer.shared.utils.PlayerWrapper;

public interface SRApplier {
    void applySkin(PlayerWrapper playerWrapper, String str) throws SkinRequestException;
}
