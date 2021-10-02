package net.skinsrestorer.shared.utils;

import net.skinsrestorer.shared.interfaces.ISRPlayer;

public abstract class WrapperFactory {
    public abstract ISRPlayer wrap(Object playerInstance);
}
