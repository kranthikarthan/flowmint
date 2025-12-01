package ai.flowmint.flink.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSinkBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility to emit FileCompletion events to an outbox topic.
 * This is a stub that produces a small JSON payload with file metadata.
 */
public final class KafkaOutbox {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaOutbox.class);

    private KafkaOutbox() {}

    /**
     * Build a serialized JSON string representing a file completion event.
     */
    public static String buildFileCompletionEvent(
            String fileId,
            long recordCount,
            long rejectedCount
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "FileCompletion");
            payload.put("fileId", fileId);
            payload.put("recordCount", recordCount);
            payload.put("rejectedCount", rejectedCount);
            payload.put("timestamp", System.currentTimeMillis());
            return mapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build FileCompletion event", e);
        }
    }
}


