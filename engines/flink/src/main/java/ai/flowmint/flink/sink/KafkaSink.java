package ai.flowmint.flink.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSinkBuilder;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Kafka sink factory with exactly-once delivery guarantee.
 */
public class KafkaSink {
    
    private static final Logger LOG = LoggerFactory.getLogger(KafkaSink.class);
    
    /**
     * Create a Kafka sink with exactly-once semantics.
     *
     * @param bootstrapServers Kafka bootstrap servers
     * @param topic target topic
     * @return configured KafkaSink
     */
    public static org.apache.flink.connector.kafka.sink.KafkaSink<String> createExactlyOnceSink(
            String bootstrapServers,
            String topic) {
        
        LOG.info("Creating Kafka sink with exactly-once semantics for topic: {}", topic);
        
        return new KafkaSinkBuilder<String>()
                .setBootstrapServers(bootstrapServers)
                .setRecordSerializer(
                        KafkaRecordSerializationSchema.builder()
                                .setTopic(topic)
                                .setValueSerializationSchema(new SimpleStringSchema())
                                .build()
                )
                .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                .setTransactionalIdPrefix("flowmint-")
                .setProperty("transaction.timeout.ms", "900000") // 15 minutes
                .build();
    }

    /**
     * Create a Kafka sink with exactly-once semantics for JSON objects.
     * Accepts an ObjectMapper to ensure consistent serialization across the app.
     */
    public static <T> org.apache.flink.connector.kafka.sink.KafkaSink<T> createExactlyOnceJsonSink(
            String bootstrapServers,
            String topic,
            ObjectMapper objectMapper) {

        LOG.info("Creating Kafka JSON sink (exactly-once) for topic: {}", topic);

        SerializationSchema<T> jsonSchema = new JsonSerializationSchema<>(objectMapper);

        return new KafkaSinkBuilder<T>()
                .setBootstrapServers(bootstrapServers)
                .setRecordSerializer(
                        KafkaRecordSerializationSchema.<T>builder()
                                .setTopic(topic)
                                .setValueSerializationSchema(jsonSchema)
                                .build()
                )
                .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                .setTransactionalIdPrefix("flowmint-")
                .setProperty("transaction.timeout.ms", "900000")
                .build();
    }
    
    /**
     * Create a legacy Kafka producer sink (for compatibility with older Flink versions).
     *
     * @param bootstrapServers Kafka bootstrap servers
     * @param topic target topic
     * @return configured FlinkKafkaProducer
     */
    @Deprecated
    public static FlinkKafkaProducer<String> createLegacySink(
            String bootstrapServers,
            String topic) {
        
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", bootstrapServers);
        props.setProperty("transaction.timeout.ms", "900000");
        
        return new FlinkKafkaProducer<>(
                topic,
                new SimpleStringSchema(),
                props,
                FlinkKafkaProducer.Semantic.EXACTLY_ONCE
        );
    }
    
    /**
     * Add Kafka sink to a data stream.
     *
     * @param stream data stream to sink
     * @param bootstrapServers Kafka bootstrap servers
     * @param topic target topic
     */
    public static void addSinkToStream(
            DataStream<String> stream,
            String bootstrapServers,
            String topic) {
        
        org.apache.flink.connector.kafka.sink.KafkaSink<String> sink =
                createExactlyOnceSink(bootstrapServers, topic);
        
        stream.sinkTo(sink).name("KafkaSink-" + topic);
    }

    /**
     * Add a JSON Kafka sink to a data stream.
     */
    public static <T> void addJsonSinkToStream(
            DataStream<T> stream,
            String bootstrapServers,
            String topic,
            ObjectMapper objectMapper) {

        org.apache.flink.connector.kafka.sink.KafkaSink<T> sink =
                createExactlyOnceJsonSink(bootstrapServers, topic, objectMapper);

        stream.sinkTo(sink).name("KafkaJsonSink-" + topic);
    }

    /**
     * Minimal JSON serialization schema using Jackson.
     */
    private static class JsonSerializationSchema<T> implements SerializationSchema<T> {
        private static final long serialVersionUID = 1L;
        private transient ObjectMapper mapper;

        JsonSerializationSchema(ObjectMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public void open(InitializationContext context) {
            if (this.mapper == null) {
                this.mapper = new ObjectMapper();
            }
        }

        @Override
        public byte[] serialize(T element) {
            try {
                return mapper.writeValueAsBytes(element);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize to JSON", e);
            }
        }
    }
}

