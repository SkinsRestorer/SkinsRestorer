import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.connections.ServiceCheckerService;
import net.skinsrestorer.shared.log.SRLogLevel;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.log.SRPlatformLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.serverinfo.Platform;
import net.skinsrestorer.shared.update.SharedUpdateCheck;
import net.skinsrestorer.shared.utils.MetricsCounter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
public class ServicesTest {
    @Mock
    private SRPlatformAdapter srPlatformAdapter;
    @Mock
    private SkinsRestorerLocale skinsRestorerLocale;

    @BeforeAll
    public static void setup() {
        System.setProperty("sr.unit.test", "true");
    }

    @Test
    public void testServices() {
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
        injector.register(SRPlatformAdapter.class, srPlatformAdapter);
        new SRPlugin(injector, "UnitTest", null, Platform.BUKKIT, SharedUpdateCheck.class);

        MetricsCounter metricsCounter = injector.getSingleton(MetricsCounter.class);
        ServiceCheckerService.ServiceCheckResponse serviceChecker = injector.getSingleton(ServiceCheckerService.class).checkServices();

        serviceChecker.getResults().forEach(System.out::println);

        assertFalse(serviceChecker.getResults().isEmpty());
        assertEquals(3, serviceChecker.getWorkingUUID());
        assertEquals(3, serviceChecker.getWorkingProfile());

        assertEquals(2, metricsCounter.collect(MetricsCounter.Service.ASHCON));
        assertEquals(2, metricsCounter.collect(MetricsCounter.Service.MINE_TOOLS));
        assertEquals(2, metricsCounter.collect(MetricsCounter.Service.MOJANG));
    }
}
