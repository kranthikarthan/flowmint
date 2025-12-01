package ai.flowmint.flink.dedup;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DedupState.
 */
class DedupStateTest {
    
    @Test
    void testDeduplication() {
        // Test that DedupState can be instantiated
        DedupState dedupState = new DedupState.Builder()
                .withTtl(1, TimeUnit.HOURS)
                .build();
        
        assertNotNull(dedupState);
    }
    
    @Test
    void testCreateKey() {
        assertEquals("file1|txn1", DedupState.createKey("file1", "txn1"));
        assertEquals("|txn1", DedupState.createKey(null, "txn1"));
        assertEquals("file1|", DedupState.createKey("file1", null));
        assertEquals("|", DedupState.createKey(null, null));
    }
    
    @Test
    void testDedupRecord() {
        DedupState.DedupRecord record = new DedupState.DedupRecord("file1", "txn1", "data");
        
        assertEquals("file1", record.getFileId());
        assertEquals("txn1", record.getTxnId());
        assertEquals("data", record.getData());
        assertFalse(record.isDuplicate());
        assertEquals("file1|txn1", record.getKey());
        
        record.setDuplicate(true);
        assertTrue(record.isDuplicate());
    }
    
    @Test
    void testBuilder() {
        DedupState dedupState = new DedupState.Builder()
                .withTtl(24, TimeUnit.HOURS)
                .build();
        
        assertNotNull(dedupState);
    }
}

