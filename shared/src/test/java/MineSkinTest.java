import ch.jalu.configme.SettingsManager;
import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.config.APIConfig;
import net.skinsrestorer.shared.connections.MineSkinAPIImpl;
import net.skinsrestorer.shared.interfaces.SRPlatformLogger;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.log.SRLogLevel;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MineSkinTest {
    @Mock
    private SettingsManager settingsManager;
    @Mock
    private SkinsRestorerLocale skinsRestorerLocale;

    private static final String TEST_URL = "https://skinsrestorer.net/skinsrestorer-skin.png";

    @BeforeAll
    public static void setup() {
        System.setProperty("sr.unit.test", "true");
    }

    @Test
    public void testServices() throws DataRequestException {
        Injector injector = new InjectorBuilder().addDefaultHandlers("net.skinsrestorer").create();

        SRLogger logger = new SRLogger(new SRPlatformLogger() {
            @Override
            public void log(SRLogLevel level, String message) {
                System.out.println(level + " " + message);
            }

            @Override
            public void log(SRLogLevel level, String message, Throwable throwable) {
                System.out.println(level + " " + message);
                throwable.printStackTrace();
            }
        }, false);

        injector.register(SRLogger.class, logger);
        injector.register(SkinsRestorerLocale.class, skinsRestorerLocale);

        when(settingsManager.getProperty(APIConfig.MINESKIN_API_KEY)).thenReturn("");

        injector.register(SettingsManager.class, settingsManager);

        MetricsCounter metricsCounter = injector.getSingleton(MetricsCounter.class);
        SkinProperty skinProperty = injector.getSingleton(MineSkinAPIImpl.class).genSkin(TEST_URL, null);

        assertNotNull(skinProperty);

        assertEquals(1, metricsCounter.collect(MetricsCounter.Service.MINE_SKIN));
    }
}
