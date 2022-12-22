import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import net.skinsrestorer.api.interfaces.IPropertyFactory;
import net.skinsrestorer.api.property.GenericProperty;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.interfaces.ISRLogger;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.connections.ServiceChecker;
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

        SRLogger logger = new SRLogger(new ISRLogger() {
            @Override
            public void log(SRLogLevel level, String message) {
                System.out.println(level + " " + message);
            }

            @Override
            public void log(SRLogLevel level, String message, Throwable throwable) {
                System.out.println(level + " " + message);
                throwable.printStackTrace();
            }
        });

        injector.register(SRLogger.class, logger);
        injector.register(SkinsRestorerLocale.class, mock(SkinsRestorerLocale.class));
        injector.register(IPropertyFactory.class, (name, value, signature) -> {
            System.out.println("Property: " + name + " " + value + " " + signature);
            return new GenericProperty(name, value, signature);
        });

        MetricsCounter metricsCounter = injector.getSingleton(MetricsCounter.class);
        ServiceChecker.ServiceCheckResponse serviceChecker = ServiceChecker.checkServices(injector.getSingleton(MojangAPI.class));

        serviceChecker.getResults().forEach(System.out::println);

        assertFalse(serviceChecker.getResults().isEmpty());
        assertEquals(3, serviceChecker.getWorkingUUID());
        assertEquals(3, serviceChecker.getWorkingProfile());

        assertEquals(2, metricsCounter.collect(MetricsCounter.Service.ASHCON));
        assertEquals(2, metricsCounter.collect(MetricsCounter.Service.MINE_TOOLS));
        assertEquals(2, metricsCounter.collect(MetricsCounter.Service.MOJANG));
    }
}
