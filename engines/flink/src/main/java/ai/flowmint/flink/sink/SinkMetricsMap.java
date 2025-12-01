package ai.flowmint.flink.sink;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;

/**
 * Utility map function to track sink metrics before sending to a sink.
 * Increments sink_success on pass-through. For retries/failures, wrap downstream.
 */
public class SinkMetricsMap<T> extends RichMapFunction<T, T> {
    private transient org.apache.flink.metrics.Counter sinkSuccess;

    @Override
    public void open(Configuration parameters) {
        sinkSuccess = getRuntimeContext().getMetricGroup().counter("sink_success");
    }

    @Override
    public T map(T value) {
        sinkSuccess.inc();
        return value;
    }
}


