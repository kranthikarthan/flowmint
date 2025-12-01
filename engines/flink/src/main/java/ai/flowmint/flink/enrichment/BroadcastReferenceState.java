package ai.flowmint.flink.enrichment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * BroadcastReferenceState manages loading and refreshing reference data
 * with TTL support from local files or Kafka topics (stub).
 * 
 * Provides accessors for enrichment fields in a functional style.
 */
public class BroadcastReferenceState {
    
    private static final Logger LOG = LoggerFactory.getLogger(BroadcastReferenceState.class);
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    
    private final String filePath;
    private final long refreshIntervalMs;
    private final long defaultTtlMs;
    private long lastRefreshTime;
    
    public BroadcastReferenceState(String filePath, long refreshIntervalMs, long defaultTtlMs) {
        this.filePath = filePath;
        this.refreshIntervalMs = refreshIntervalMs;
        this.defaultTtlMs = defaultTtlMs;
        this.lastRefreshTime = 0;
    }
    
    /**
     * Load reference data from local file.
     * 
     * @return Map of reference data keyed by enrichment field
     */
    public Map<String, ReferenceData> loadFromFile() throws IOException {
        LOG.info("Loading reference data from file: {}", filePath);
        
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            LOG.warn("Reference data file not found: {}", filePath);
            return new HashMap<>();
        }
        
        String content = Files.readString(path);
        return parseReferenceData(content);
    }
    
    /**
     * Parse reference data from JSON/YAML content.
     */
    private Map<String, ReferenceData> parseReferenceData(String content) throws IOException {
        Map<String, ReferenceData> referenceDataMap = new HashMap<>();
        
        try {
            // Try parsing as JSON
            Map<String, Object> data = JSON_MAPPER.readValue(content, Map.class);
            
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                if (value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> valueMap = (Map<String, Object>) value;
                    
                    // Extract TTL if present, otherwise use default
                    long ttl = defaultTtlMs;
                    if (valueMap.containsKey("ttl")) {
                        Object ttlObj = valueMap.get("ttl");
                        if (ttlObj instanceof Number) {
                            ttl = ((Number) ttlObj).longValue();
                        }
                    }
                    
                    // Remove TTL from data map
                    Map<String, Object> dataMap = new HashMap<>(valueMap);
                    dataMap.remove("ttl");
                    
                    ReferenceData refData = new ReferenceData(
                            key,
                            dataMap,
                            System.currentTimeMillis(),
                            ttl
                    );
                    
                    referenceDataMap.put(key, refData);
                }
            }
            
            LOG.info("Loaded {} reference data entries", referenceDataMap.size());
            
        } catch (Exception e) {
            LOG.error("Failed to parse reference data", e);
            throw new IOException("Failed to parse reference data", e);
        }
        
        return referenceDataMap;
    }
    
    /**
     * Check if refresh is needed based on refresh interval.
     */
    public boolean needsRefresh() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastRefreshTime) >= refreshIntervalMs;
    }
    
    /**
     * Mark refresh as completed.
     */
    public void markRefreshed() {
        this.lastRefreshTime = System.currentTimeMillis();
    }
    
    /**
     * Load reference data from Kafka topic (stub implementation).
     * 
     * @param topic Kafka topic name
     * @return SourceFunction for reading reference data from Kafka
     */
    public SourceFunction<ReferenceData> createKafkaSource(String topic) {
        LOG.info("Creating Kafka source for reference data topic: {} (stub)", topic);
        
        // Stub implementation - returns empty source
        // In production, this would create a KafkaSource
        return new SourceFunction<ReferenceData>() {
            private volatile boolean running = true;
            
            @Override
            public void run(SourceContext<ReferenceData> ctx) {
                // Stub: do nothing for now
                // TODO: Implement actual Kafka consumer
                LOG.warn("Kafka source for reference data is a stub. Not reading from topic: {}", topic);
            }
            
            @Override
            public void cancel() {
                running = false;
            }
        };
    }
    
    /**
     * Accessor: Get routing information for a currency.
     * 
     * @param broadcastState broadcast state
     * @param currency currency code
     * @return routing data map or null if not found
     */
    public static Map<String, Object> getRoutingInfo(
            ReadOnlyBroadcastState<String, ReferenceData> broadcastState,
            String currency) {
        
        if (currency == null || currency.isEmpty()) {
            return null;
        }
        
        String key = "routing_" + currency;
        ReferenceData refData = broadcastState.get(key);
        
        if (refData != null && !refData.isExpired(System.currentTimeMillis())) {
            return refData.getData();
        }
        
        return null;
    }
    
    /**
     * Accessor: Get limits information.
     * 
     * @param broadcastState broadcast state
     * @return limits data map or null if not found
     */
    public static Map<String, Object> getLimits(
            ReadOnlyBroadcastState<String, ReferenceData> broadcastState) {
        
        ReferenceData refData = broadcastState.get("limits");
        
        if (refData != null && !refData.isExpired(System.currentTimeMillis())) {
            return refData.getData();
        }
        
        return null;
    }
    
    /**
     * Accessor: Get any enrichment field by key.
     * 
     * @param broadcastState broadcast state
     * @param key enrichment field key
     * @return enrichment data map or null if not found
     */
    public static Map<String, Object> getEnrichmentField(
            ReadOnlyBroadcastState<String, ReferenceData> broadcastState,
            String key) {
        
        if (key == null || key.isEmpty()) {
            return null;
        }
        
        ReferenceData refData = broadcastState.get(key);
        
        if (refData != null && !refData.isExpired(System.currentTimeMillis())) {
            return refData.getData();
        }
        
        return null;
    }
    
    /**
     * Builder for creating BroadcastReferenceState instances.
     */
    public static class Builder {
        private String filePath;
        private long refreshIntervalMs = TimeUnit.MINUTES.toMillis(5);
        private long defaultTtlMs = TimeUnit.HOURS.toMillis(1);
        
        public Builder withFilePath(String filePath) {
            this.filePath = filePath;
            return this;
        }
        
        public Builder withRefreshInterval(long interval, TimeUnit unit) {
            this.refreshIntervalMs = unit.toMillis(interval);
            return this;
        }
        
        public Builder withDefaultTtl(long ttl, TimeUnit unit) {
            this.defaultTtlMs = unit.toMillis(ttl);
            return this;
        }
        
        public BroadcastReferenceState build() {
            if (filePath == null || filePath.isEmpty()) {
                throw new IllegalArgumentException("File path is required");
            }
            return new BroadcastReferenceState(filePath, refreshIntervalMs, defaultTtlMs);
        }
    }
}

