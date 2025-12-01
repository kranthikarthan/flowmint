package ai.flowmint.flink.sink;

import ai.flowmint.flink.model.Lineage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LineageSinkTest {

    @Test
    public void testLineageSink(@TempDir Path tempDir) throws Exception {
        String fileId = "test-file";
        String fileHash = "test-hash";
        Lineage lineage = new Lineage(fileId, fileHash);
        lineage.setMappingVersion("1.0");
        lineage.setUdfVersionHash("udf-hash");

        LineageSink sink = new LineageSink(tempDir.toString());
        sink.invoke(lineage, null);

        File expectedFile = new File(tempDir.toFile(), fileId + "-lineage.json");
        assertTrue(expectedFile.exists());

        String content = new String(Files.readAllBytes(expectedFile.toPath()));
        assertTrue(content.contains("\"fileId\": \"test-file\""));
        assertTrue(content.contains("\"fileHash\": \"test-hash\""));
    }
}
