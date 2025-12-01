package ai.flowmint.flink.sink;

import ai.flowmint.flink.model.CanonicalPayment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Sink interface for canonical payment records.
 * 
 * Provides in-memory and logger implementations.
 * Later replace with Kafka/Iceberg implementations.
 */
public interface CanonicalPaymentSink extends SinkFunction<CanonicalPayment> {
    
    /**
     * In-memory sink implementation for testing and development.
     */
    class InMemorySink implements CanonicalPaymentSink {
        private static final ConcurrentMap<String, List<CanonicalPayment>> storage = new ConcurrentHashMap<>();
        
        @Override
        public void invoke(CanonicalPayment value, Context context) {
            String key = value.getFileId() != null ? value.getFileId() : "default";
            storage.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
        
        public List<CanonicalPayment> getPayments(String fileId) {
            return storage.getOrDefault(fileId, new ArrayList<>());
        }
        
        public List<CanonicalPayment> getAllPayments() {
            List<CanonicalPayment> all = new ArrayList<>();
            storage.values().forEach(all::addAll);
            return all;
        }
        
        public void clear() {
            storage.clear();
        }
        
        public int size() {
            return storage.values().stream().mapToInt(List::size).sum();
        }
    }
    
    /**
     * Logger sink implementation for development and debugging.
     */
    class LoggerSink implements CanonicalPaymentSink {
        private static final Logger LOG = LoggerFactory.getLogger(LoggerSink.class);
        
        @Override
        public void invoke(CanonicalPayment value, Context context) {
            LOG.info("CanonicalPayment: {}", value);
        }
    }
    
    /**
     * Factory for creating sink instances.
     */
    class Factory {
        public static CanonicalPaymentSink createInMemory() {
            return new InMemorySink();
        }
        
        public static CanonicalPaymentSink createLogger() {
            return new LoggerSink();
        }
        
        // Future implementations:
        // public static CanonicalPaymentSink createKafka(...) { ... }
        // public static CanonicalPaymentSink createIceberg(...) { ... }
    }
}

