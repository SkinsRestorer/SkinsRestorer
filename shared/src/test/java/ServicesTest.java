import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.connections.ServiceCheckerService;
import net.skinsrestorer.shared.interfaces.SRPlatformAdapter;
import net.skinsrestorer.shared.interfaces.SRPlatformLogger;
import net.skinsrestorer.shared.platform.SRPlugin;
import net.skinsrestorer.shared.serverinfo.Platform;
import net.skinsrestorer.shared.update.SharedUpdateCheck;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.log.SRLogLevel;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

public class ServicesTest {
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
        injector.register(SkinsRestorerLocale.class, mock(SkinsRestorerLocale.class));
        injector.register(SRPlatformAdapter.class, mock(SRPlatformAdapter.class));
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
