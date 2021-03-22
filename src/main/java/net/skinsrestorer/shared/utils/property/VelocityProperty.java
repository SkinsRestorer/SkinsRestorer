package net.skinsrestorer.shared.utils.property;

import com.velocitypowered.api.util.GameProfile.Property;

public class VelocityProperty implements IProperty {
    private final Property property;

    public VelocityProperty(String name, String value, String signature) {
        property = new Property(name, value, signature);
    }


    @Override
    public Object getHandle() {
        return property;
    }

    @Override
    public String getName() {
        return property.getName();
    }

    @Override
    public String getValue() {
        return property.getValue();
    }

    @Override
    public String getSignature() {
        return property.getSignature();
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
