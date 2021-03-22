package net.skinsrestorer.shared.utils.property;

import net.md_5.bungee.connection.LoginResult.Property;

public class BungeeProperty extends Property implements IProperty {
    public BungeeProperty(String name, String value, String signature) {
        super(name, value, signature);
    }

    @Override
    public Object getHandle() {
        return this;
    }
}
