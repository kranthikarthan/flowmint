package ai.flowmint.flink.enrichment;

import java.io.Serializable;
import java.util.Map;

/**
 * Reference data for enrichment (routing tables, limits, etc.).
 */
public class ReferenceData implements Serializable {
    
    private String key;
    private Map<String, Object> data;
    private long timestamp;
    private long ttl; // Time to live in milliseconds
    
    public ReferenceData() {
    }
    
    public ReferenceData(String key, Map<String, Object> data, long timestamp, long ttl) {
        this.key = key;
        this.data = data;
        this.timestamp = timestamp;
        this.ttl = ttl;
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public long getTtl() {
        return ttl;
    }
    
    public void setTtl(long ttl) {
        this.ttl = ttl;
    }
    
    public boolean isExpired(long currentTime) {
        return (currentTime - timestamp) > ttl;
    }
    
    @Override
    public String toString() {
        return "ReferenceData{" +
                "key='" + key + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                ", ttl=" + ttl +
                '}';
    }
}

