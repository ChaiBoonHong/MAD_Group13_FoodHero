package com.uccd3223.group13.foodhero;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.uccd3223.group13.foodhero.data.model.OrderStatus;
import org.junit.Test;

public class OrderStatusContractTest {
    @Test public void parsesAuthoritativeStates() {
        assertEquals(OrderStatus.READY_FOR_PICKUP, OrderStatus.fromString("ready_for_pickup"));
        assertEquals(OrderStatus.PAYMENT_REJECTED, OrderStatus.fromString("payment_rejected"));
        assertEquals(OrderStatus.NO_SHOW, OrderStatus.fromString("no_show"));
    }

    @Test public void mapsLegacyStatesDuringMigration() {
        assertEquals(OrderStatus.READY_FOR_PICKUP, OrderStatus.fromString("reserved"));
        assertEquals(OrderStatus.PAYMENT_REJECTED, OrderStatus.fromString("rejected"));
    }

    @Test public void unknownStateFailsClosed() {
        assertNull(OrderStatus.fromString("future_untrusted_state"));
        assertNull(OrderStatus.fromString(null));
    }
}
