package ai.flowmint.flink.sink;

import ai.flowmint.flink.model.Lineage;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LineageSink implements SinkFunction<Lineage> {

    private static final Logger LOG = LoggerFactory.getLogger(LineageSink.class);
    private final String outputPath;

    public LineageSink(String outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public void invoke(Lineage lineage, Context context) throws Exception {
        lineage.markCompleted();
        String lineageJson = createLineageJson(lineage);

        File outputFile = new File(outputPath, lineage.getFileId() + "-lineage.json");
        outputFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(lineageJson);
        } catch (IOException e) {
            LOG.error("Failed to write lineage to file", e);
        }
    }

    private String createLineageJson(Lineage lineage) {
        return String.format("{\n" +
                "  \"fileId\": \"%s\",\n" +
                "  \"fileHash\": \"%s\",\n" +
                "  \"ingestTime\": \"%s\",\n" +
                "  \"completionTime\": \"%s\",\n" +
                "  \"recordCount\": %d,\n" +
                "  \"rejectedCount\": %d,\n" +
                "  \"mappingVersion\": \"%s\",\n" +
                "  \"udfVersionHash\": \"%s\"\n" +
                "}",
            lineage.getFileId(),
            lineage.getFileHash(),
            lineage.getIngestTime(),
            lineage.getCompletionTime(),
            lineage.getRecordCount(),
            lineage.getRejectedCount(),
            lineage.getMappingVersion(),
            lineage.getUdfVersionHash()
        );
    }
}
