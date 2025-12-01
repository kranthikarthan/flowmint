package ai.flowmint.flink.enrichment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BroadcastReferenceState.
 */
class BroadcastReferenceStateTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void testLoadFromFile() throws Exception {
        // Create test reference data file
        Path refDataFile = tempDir.resolve("reference-data.json");
        String json = """
                {
                    "routing_USD": {
                        "bankCode": "CHASUS33",
                        "swiftCode": "CHASUS33",
                        "ttl": 3600000
                    },
                    "limits": {
                        "maxAmount": 1000000,
                        "currency": "USD"
                    }
                }
                """;
        Files.writeString(refDataFile, json);
        
        BroadcastReferenceState state = new BroadcastReferenceState.Builder()
                .withFilePath(refDataFile.toString())
                .withDefaultTtl(1, TimeUnit.HOURS)
                .build();
        
        Map<String, ReferenceData> data = state.loadFromFile();
        
        assertNotNull(data);
        assertEquals(2, data.size());
        assertTrue(data.containsKey("routing_USD"));
        assertTrue(data.containsKey("limits"));
        
        ReferenceData routingData = data.get("routing_USD");
        assertEquals("routing_USD", routingData.getKey());
        assertTrue(routingData.getData().containsKey("bankCode"));
        assertEquals(3600000L, routingData.getTtl());
    }
    
    @Test
    void testNeedsRefresh() throws Exception {
        Path refDataFile = tempDir.resolve("reference-data.json");
        Files.writeString(refDataFile, "{}");
        
        BroadcastReferenceState state = new BroadcastReferenceState.Builder()
                .withFilePath(refDataFile.toString())
                .withRefreshInterval(1, TimeUnit.SECONDS)
                .build();
        
        assertTrue(state.needsRefresh());
        
        state.markRefreshed();
        assertFalse(state.needsRefresh());
        
        Thread.sleep(1100);
        assertTrue(state.needsRefresh());
    }
    
    @Test
    void testBuilder() {
        BroadcastReferenceState.Builder builder = new BroadcastReferenceState.Builder();
        
        assertThrows(IllegalArgumentException.class, builder::build);
        
        BroadcastReferenceState state = builder
                .withFilePath("/test/path")
                .withRefreshInterval(5, TimeUnit.MINUTES)
                .withDefaultTtl(2, TimeUnit.HOURS)
                .build();
        
        assertNotNull(state);
    }
}

