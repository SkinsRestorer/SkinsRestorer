package net.skinsrestorer;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.properties.Property;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

public class SettingsHelper {
    @SuppressWarnings("unchecked")
    public static void returnDefaultsForAllProperties(SettingsManager settings) {
        given(settings.getProperty(any(Property.class)))
                .willAnswer(invocation -> ((Property<?>) invocation.getArgument(0)).getDefaultValue());
    }
}
