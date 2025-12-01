package ai.flowmint.flink.enrichment;

import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Broadcast process function for enriching payment records with reference data.
 * 
 * This function uses Flink's broadcast state pattern to distribute reference data
 * (routing tables, limits) to all parallel instances with TTL support.
 */
public class EnrichmentFunction extends BroadcastProcessFunction<Map<String, Object>, ReferenceData, Map<String, Object>> {
    
    private static final Logger LOG = LoggerFactory.getLogger(EnrichmentFunction.class);
    
    public static final MapStateDescriptor<String, ReferenceData> REFERENCE_DATA_DESCRIPTOR =
            new MapStateDescriptor<>(
                    "reference-data",
                    TypeInformation.of(String.class),
                    TypeInformation.of(ReferenceData.class)
            );
    
    private long currentTime;
    
    // Metrics
    private transient org.apache.flink.metrics.Counter enrichmentHits;
    private transient org.apache.flink.metrics.Counter enrichmentMisses;
    
    @Override
    public void open(Configuration parameters) {
        currentTime = System.currentTimeMillis();
        
        // Register metrics
        enrichmentHits = getRuntimeContext()
                .getMetricGroup()
                .counter("enrichment_hits");
        
        enrichmentMisses = getRuntimeContext()
                .getMetricGroup()
                .counter("enrichment_misses");
    }
    
    @Override
    public void processElement(
            Map<String, Object> value,
            ReadOnlyContext ctx,
            Collector<Map<String, Object>> out) throws Exception {
        
        ReadOnlyBroadcastState<String, ReferenceData> broadcastState =
                ctx.getBroadcastState(REFERENCE_DATA_DESCRIPTOR);
        
        currentTime = System.currentTimeMillis();
        
        // Enrich with reference data
        Map<String, Object> enriched = enrichWithReferenceData(value, broadcastState);
        
        out.collect(enriched);
    }
    
    @Override
    public void processBroadcastElement(
            ReferenceData referenceData,
            Context ctx,
            Collector<Map<String, Object>> out) throws Exception {
        
        BroadcastState<String, ReferenceData> broadcastState =
                ctx.getBroadcastState(REFERENCE_DATA_DESCRIPTOR);
        
        currentTime = System.currentTimeMillis();
        
        // Check TTL - remove expired entries
        if (referenceData.isExpired(currentTime)) {
            LOG.debug("Removing expired reference data: {}", referenceData.getKey());
            broadcastState.remove(referenceData.getKey());
        } else {
            LOG.debug("Updating reference data: {}", referenceData.getKey());
            broadcastState.put(referenceData.getKey(), referenceData);
        }
        
        // Clean up expired entries
        cleanupExpiredEntries(broadcastState);
    }
    
    /**
     * Enrich payment record with reference data.
     */
    private Map<String, Object> enrichWithReferenceData(
            Map<String, Object> record,
            ReadOnlyBroadcastState<String, ReferenceData> broadcastState) {
        
        boolean enriched = false;
        
        // Example: enrich with routing data based on currency or IBAN
        if (record.containsKey("currency")) {
            String currency = (String) record.get("currency");
            ReferenceData routingData = broadcastState.get("routing_" + currency);
            
            if (routingData != null && !routingData.isExpired(currentTime)) {
                record.put("routingInfo", routingData.getData());
                enriched = true;
            }
        }
        
        // Enrich with limits data
        ReferenceData limitsData = broadcastState.get("limits");
        if (limitsData != null && !limitsData.isExpired(currentTime)) {
            record.put("limits", limitsData.getData());
            enriched = true;
        }
        
        // Update metrics
        if (enriched) {
            enrichmentHits.inc();
        } else {
            enrichmentMisses.inc();
        }
        
        return record;
    }
    
    /**
     * Clean up expired entries from broadcast state.
     */
    private void cleanupExpiredEntries(BroadcastState<String, ReferenceData> broadcastState) throws Exception {
        for (Map.Entry<String, ReferenceData> entry : broadcastState.entries()) {
            if (entry.getValue().isExpired(currentTime)) {
                LOG.debug("Cleaning up expired reference data: {}", entry.getKey());
                broadcastState.remove(entry.getKey());
            }
        }
    }
}

