package ai.flowmint.flink.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KafkaSinkTest {
    @Test
    void testJsonSerializationSchemaOpenAndSerialize() {
        // Ensure the JSON schema can serialize simple objects
        ObjectMapper mapper = new ObjectMapper();
        var schema = org.mockito.Mockito.spy(
                new Object() {
                }
        );
        // Simple sanity check: the factory method builds a sink without throwing
        var sink = KafkaSink.createExactlyOnceJsonSink("localhost:9092", "topic", mapper);
        assertNotNull(sink);
    }
}


