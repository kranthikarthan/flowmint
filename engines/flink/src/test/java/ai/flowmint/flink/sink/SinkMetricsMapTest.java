package ai.flowmint.flink.sink;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SinkMetricsMapTest {
    @Test
    void instantiate() {
        SinkMetricsMap<String> f = new SinkMetricsMap<>();
        assertNotNull(f);
    }
}


