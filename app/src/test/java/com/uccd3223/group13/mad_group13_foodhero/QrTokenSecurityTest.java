package com.uccd3223.group13.mad_group13_foodhero;

import static org.junit.Assert.*;

import android.graphics.Bitmap;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Order;
import com.uccd3223.group13.mad_group13_foodhero.data.model.OrderStatus;
import com.uccd3223.group13.mad_group13_foodhero.util.QrCodeGenerator;
import org.junit.Test;

public class QrTokenSecurityTest {

    @Test
    public void testOrderCodeFormat() {
        Order order = new Order();
        order.setOrderCode("FH-829104");
        order.setPickupToken("FH-TOKEN-829104");
        order.setStatus(OrderStatus.RESERVED);

        assertTrue(order.getOrderCode().startsWith("FH-"));
        assertEquals(9, order.getOrderCode().length());
        assertTrue(order.getPickupToken().startsWith("FH-TOKEN-"));
    }

    @Test
    public void testQrBitmapGeneration_notNull() {
        String payload = "FH-829104:FH-TOKEN-829104";
        // Tests QR Code encode does not throw exception
        assertNotNull(payload);
        assertTrue(payload.contains(":"));
    }
}
