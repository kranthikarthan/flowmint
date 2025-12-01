package ai.flowmint.flink.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CanonicalPayment.
 */
class CanonicalPaymentTest {
    
    @Test
    void testCanonicalPayment() {
        CanonicalPayment payment = new CanonicalPayment();
        payment.setTxnId("txn1");
        payment.setAmount(new BigDecimal("100.50"));
        payment.setCurrency("USD");
        payment.setRail("ISO20022");
        payment.setFileId("file1");
        payment.setDebtorIban("DE89370400440532013000");
        payment.setCreditorIban("GB82WEST12345698765432");
        payment.setEndToEndId("E2E123");
        
        assertEquals("txn1", payment.getTxnId());
        assertEquals(new BigDecimal("100.50"), payment.getAmount());
        assertEquals("USD", payment.getCurrency());
        assertEquals("ISO20022", payment.getRail());
        assertEquals("file1", payment.getFileId());
        assertNotNull(payment.getProcessedAt());
    }
    
    @Test
    void testCanonicalPaymentConstructor() {
        CanonicalPayment payment = new CanonicalPayment(
                "txn1",
                new BigDecimal("100.50"),
                "USD",
                "ISO20022"
        );
        
        assertEquals("txn1", payment.getTxnId());
        assertEquals(new BigDecimal("100.50"), payment.getAmount());
        assertEquals("USD", payment.getCurrency());
        assertEquals("ISO20022", payment.getRail());
        assertNotNull(payment.getProcessedAt());
    }
    
    @Test
    void testToString() {
        CanonicalPayment payment = new CanonicalPayment(
                "txn1",
                new BigDecimal("100.50"),
                "USD",
                "ISO20022"
        );
        payment.setFileId("file1");
        
        String str = payment.toString();
        assertTrue(str.contains("txn1"));
        assertTrue(str.contains("100.50"));
        assertTrue(str.contains("USD"));
    }
}

