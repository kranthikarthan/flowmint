package ai.flowmint.flink.sink;

import ai.flowmint.flink.model.CanonicalPayment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Iceberg sink interface + stub implementation.
 * This is a placeholder for future integration with Apache Iceberg.
 */
public final class IcebergSink {
    private static final Logger LOG = LoggerFactory.getLogger(IcebergSink.class);

    private IcebergSink() {}

    /**
     * Stub for writing canonical payments to an Iceberg table.
     */
    public static void addCanonicalSink(
            DataStream<CanonicalPayment> stream,
            String catalog,
            String database,
            String table) {
        LOG.warn("Iceberg sink (canonical) is a stub. Target: {}.{}.{}", catalog, database, table);
        // TODO: Integrate Iceberg Flink sink when infra is available
    }

    /**
     * Stub for writing rejected records to a separate Iceberg table.
     */
    public static void addRejectsSink(
            DataStream<String> rejects,
            String catalog,
            String database,
            String table) {
        LOG.warn("Iceberg sink (rejects) is a stub. Target: {}.{}.{}", catalog, database, table);
    }
}


