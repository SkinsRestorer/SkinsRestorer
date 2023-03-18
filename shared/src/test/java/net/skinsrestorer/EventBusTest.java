package net.skinsrestorer;

import ch.jalu.injector.Injector;
import net.skinsrestorer.api.event.SkinApplyEvent;
import net.skinsrestorer.shared.api.event.EventBusImpl;
import net.skinsrestorer.shared.api.event.SkinApplyEventImpl;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({MockitoExtension.class, SRExtension.class})
public class EventBusTest {

    @Mock
    private SRPlatformAdapter<?> srPlatformAdapter;
    @Mock
    private Object plugin;

    @Test
    public void testServices(Injector injector) {
        injector.register(SRPlatformAdapter.class, srPlatformAdapter);

        EventBusImpl eventBus = injector.getSingleton(EventBusImpl.class);

        eventBus.subscribe(plugin, SkinApplyEvent.class, new TestListener());

        SkinApplyEvent event = new SkinApplyEventImpl(null, null);

        eventBus.callEvent(event);

        assertTrue(event.isCancelled(), "Event was not cancelled");
    }

    private static class TestListener implements Consumer<SkinApplyEvent> {
        @Override
        public void accept(SkinApplyEvent event) {
            event.setCancelled(true);
        }
    }
}
