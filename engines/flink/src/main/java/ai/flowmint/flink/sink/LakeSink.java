package ai.flowmint.flink.sink;

import org.apache.flink.api.common.serialization.Encoder;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Lakehouse sink factory for writing to data lakes (S3, HDFS, etc.).
 * 
 * This is a stub implementation that can be extended for specific lakehouse formats
 * (Parquet, Delta Lake, Iceberg, etc.).
 */
public class LakeSink {
    
    private static final Logger LOG = LoggerFactory.getLogger(LakeSink.class);
    
    /**
     * Create a file sink for lakehouse storage (stub implementation).
     *
     * @param basePath base path for output files
     * @param format format (parquet, delta, iceberg, etc.) - currently supports text/json
     * @return configured FileSink
     */
    public static FileSink<String> createLakeSink(String basePath, String format) {
        LOG.info("Creating lake sink at path: {} with format: {}", basePath, format);
        
        FileSink<String> sink = FileSink
                .forRowFormat(
                        new Path(basePath),
                        new StringEncoder()
                )
                .withRollingPolicy(
                        DefaultRollingPolicy.builder()
                                .withRolloverInterval(TimeUnit.MINUTES.toMillis(15))
                                .withMaxPartSize(128 * 1024 * 1024) // 128 MB
                                .withInactivityInterval(TimeUnit.MINUTES.toMillis(5))
                                .build()
                )
                .build();
        
        LOG.warn("Lake sink is a stub implementation. Format '{}' support to be added in future sprints.", format);
        return sink;
    }
    
    /**
     * Add lake sink to a data stream.
     *
     * @param stream data stream to sink
     * @param basePath base path for output files
     * @param format format (parquet, delta, iceberg, etc.)
     */
    public static void addSinkToStream(
            DataStream<String> stream,
            String basePath,
            String format) {
        
        FileSink<String> sink = createLakeSink(basePath, format);
        stream.sinkTo(sink).name("LakeSink-" + format);
    }
    
    /**
     * Simple string encoder for text/JSON output.
     */
    private static class StringEncoder implements Encoder<String> {
        @Override
        public void encode(String element, OutputStream stream) throws IOException {
            stream.write(element.getBytes(StandardCharsets.UTF_8));
            stream.write('\n');
        }
    }
}

