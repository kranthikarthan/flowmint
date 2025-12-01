package ai.flowmint.flink.dedup;

import org.apache.flink.api.common.state.StateTtlConfig;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.StateBackend;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * DedupState wrapper using RocksDB/KeyedState for deduplication.
 * 
 * Key = fileId + txnId
 * TTL = configurable
 * 
 * Uses Flink's ValueState with TTL to track seen transactions.
 */
public class DedupState extends KeyedProcessFunction<String, DedupRecord, DedupRecord> {
    
    private static final Logger LOG = LoggerFactory.getLogger(DedupState.class);
    
    private transient ValueState<Boolean> seenState;
    private final long ttlMs;
    
    // Metrics
    private transient org.apache.flink.metrics.Counter dedupHits;
    private transient org.apache.flink.metrics.Counter dedupNew;
    
    public DedupState(long ttlMs) {
        this.ttlMs = ttlMs;
    }
    
    @Override
    public void open(Configuration parameters) {
        // Configure state with TTL
        StateTtlConfig ttlConfig = StateTtlConfig
                .newBuilder(Time.milliseconds(ttlMs))
                .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
                .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
                .build();
        
        ValueStateDescriptor<Boolean> descriptor = new ValueStateDescriptor<>(
                "dedup-state",
                TypeInformation.of(Boolean.class)
        );
        descriptor.enableTimeToLive(ttlConfig);
        
        seenState = getRuntimeContext().getState(descriptor);
        
        // Register metrics
        dedupHits = getRuntimeContext()
                .getMetricGroup()
                .counter("dedup_hits");
        
        dedupNew = getRuntimeContext()
                .getMetricGroup()
                .counter("dedup_new");
    }
    
    @Override
    public void processElement(
            DedupRecord value,
            Context ctx,
            Collector<DedupRecord> out) throws Exception {
        
        Boolean seen = seenState.value();
        
        if (seen != null && seen) {
            // Duplicate detected
            LOG.debug("Duplicate detected for key: {}", ctx.getCurrentKey());
            dedupHits.inc();
            value.setDuplicate(true);
        } else {
            // New record
            LOG.debug("New record for key: {}", ctx.getCurrentKey());
            dedupNew.inc();
            seenState.update(true);
            value.setDuplicate(false);
        }
        
        out.collect(value);
    }
    
    /**
     * Create a deduplication key from fileId and txnId.
     * 
     * @param fileId file identifier
     * @param txnId transaction identifier
     * @return deduplication key
     */
    public static String createKey(String fileId, String txnId) {
        if (fileId == null) fileId = "";
        if (txnId == null) txnId = "";
        return fileId + "|" + txnId;
    }
    
    /**
     * Builder for creating DedupState instances.
     */
    public static class Builder {
        private long ttlMs = TimeUnit.HOURS.toMillis(24);
        
        public Builder withTtl(long ttl, TimeUnit unit) {
            this.ttlMs = unit.toMillis(ttl);
            return this;
        }
        
        public DedupState build() {
            return new DedupState(ttlMs);
        }
    }
    
    /**
     * Record wrapper for deduplication tracking.
     */
    public static class DedupRecord implements Serializable {
        private String fileId;
        private String txnId;
        private Object data;
        private boolean duplicate;
        
        public DedupRecord() {
        }
        
        public DedupRecord(String fileId, String txnId, Object data) {
            this.fileId = fileId;
            this.txnId = txnId;
            this.data = data;
            this.duplicate = false;
        }
        
        public String getFileId() {
            return fileId;
        }
        
        public void setFileId(String fileId) {
            this.fileId = fileId;
        }
        
        public String getTxnId() {
            return txnId;
        }
        
        public void setTxnId(String txnId) {
            this.txnId = txnId;
        }
        
        public Object getData() {
            return data;
        }
        
        public void setData(Object data) {
            this.data = data;
        }
        
        public boolean isDuplicate() {
            return duplicate;
        }
        
        public void setDuplicate(boolean duplicate) {
            this.duplicate = duplicate;
        }
        
        public String getKey() {
            return createKey(fileId, txnId);
        }
    }
}

