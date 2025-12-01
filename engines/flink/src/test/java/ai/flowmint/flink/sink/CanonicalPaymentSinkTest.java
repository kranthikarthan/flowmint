package ai.flowmint.flink.sink;

import ai.flowmint.flink.model.CanonicalPayment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CanonicalPaymentSink.
 */
class CanonicalPaymentSinkTest {
    
    private CanonicalPaymentSink.InMemorySink inMemorySink;
    
    @BeforeEach
    void setUp() {
        inMemorySink = new CanonicalPaymentSink.InMemorySink();
    }
    
    @Test
    void testInMemorySink() throws Exception {
        CanonicalPayment payment1 = new CanonicalPayment("txn1", new BigDecimal("100.50"), "USD", "ISO20022");
        payment1.setFileId("file1");
        
        CanonicalPayment payment2 = new CanonicalPayment("txn2", new BigDecimal("200.75"), "EUR", "EFT");
        payment2.setFileId("file1");
        
        CanonicalPayment payment3 = new CanonicalPayment("txn3", new BigDecimal("50.00"), "GBP", "SWIFT");
        payment3.setFileId("file2");
        
        SinkFunction.Context context = new SinkFunction.Context() {
            @Override
            public long currentProcessingTime() {
                return System.currentTimeMillis();
            }
            
            @Override
            public long currentWatermark() {
                return 0;
            }
            
            @Override
            public Long timestamp() {
                return null;
            }
        };
        
        inMemorySink.invoke(payment1, context);
        inMemorySink.invoke(payment2, context);
        inMemorySink.invoke(payment3, context);
        
        assertEquals(3, inMemorySink.size());
        
        List<CanonicalPayment> file1Payments = inMemorySink.getPayments("file1");
        assertEquals(2, file1Payments.size());
        
        List<CanonicalPayment> file2Payments = inMemorySink.getPayments("file2");
        assertEquals(1, file2Payments.size());
        
        List<CanonicalPayment> allPayments = inMemorySink.getAllPayments();
        assertEquals(3, allPayments.size());
        
        inMemorySink.clear();
        assertEquals(0, inMemorySink.size());
    }
    
    @Test
    void testFactory() {
        CanonicalPaymentSink inMemory = CanonicalPaymentSink.Factory.createInMemory();
        assertNotNull(inMemory);
        assertTrue(inMemory instanceof CanonicalPaymentSink.InMemorySink);
        
        CanonicalPaymentSink logger = CanonicalPaymentSink.Factory.createLogger();
        assertNotNull(logger);
        assertTrue(logger instanceof CanonicalPaymentSink.LoggerSink);
    }
}

