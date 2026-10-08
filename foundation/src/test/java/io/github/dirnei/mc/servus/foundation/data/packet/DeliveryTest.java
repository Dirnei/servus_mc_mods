package io.github.dirnei.mc.servus.foundation.data.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DeliveryTest {

    @Test
    void countsDelivered() {
        Delivery delivery = new Delivery(1, 20);
        delivery.addDelivered(8);
        delivery.addDelivered(8);
        delivery.addDelivered(4);
        assertEquals(20, delivery.delivered());
        assertEquals(Delivery.State.RUNNING, delivery.state());
    }

    @Test
    void doneOnce() {
        Delivery delivery = new Delivery(2, 8);
        delivery.done();
        delivery.fail(DeliveryReason.ROUTE_BROKEN);
        assertEquals(Delivery.State.DONE, delivery.state());
        assertNull(delivery.reason());
    }

    @Test
    void failedWithReason() {
        Delivery delivery = new Delivery(3, 8);
        delivery.fail(DeliveryReason.NO_ROUTE);
        assertTrue(delivery.ended());
        assertEquals(DeliveryReason.NO_ROUTE, delivery.reason());
    }

    @Test
    void listenersRunOnce() {
        Delivery delivery = new Delivery(4, 8);
        AtomicInteger calls = new AtomicInteger();
        delivery.onEnd(d -> calls.incrementAndGet());
        delivery.done();
        delivery.done();
        assertEquals(1, calls.get());
        delivery.onEnd(d -> calls.incrementAndGet());
        assertEquals(2, calls.get());
    }
}
