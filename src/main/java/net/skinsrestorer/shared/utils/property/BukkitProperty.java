package net.skinsrestorer.shared.utils.property;

import com.mojang.authlib.properties.Property;

public class BukkitProperty extends Property implements IProperty {
    public BukkitProperty(String name, String value) {
        super(name, value);
    }

    public BukkitProperty(String name, String value, String signature) {
        super(name, value, signature);
    }

    @Override
    public Object getHandle() {
        return this;
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSignature(String signature) {
        throw new UnsupportedOperationException();
    }
}
