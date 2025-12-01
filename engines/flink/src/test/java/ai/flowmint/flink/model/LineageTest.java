package ai.flowmint.flink.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

public class LineageTest {

    @Test
    public void testLineageCreation() {
        String fileId = "test-file";
        String fileHash = "test-hash";
        Lineage lineage = new Lineage(fileId, fileHash);

        assertEquals(fileId, lineage.getFileId());
        assertEquals(fileHash, lineage.getFileHash());
    }

    @Test
    public void testLineageCompletion() {
        String fileId = "test-file";
        String fileHash = "test-hash";
        Lineage lineage = new Lineage(fileId, fileHash);

        lineage.markCompleted();

        assertEquals(true, lineage.getCompletionTime().isAfter(lineage.getIngestTime()));
    }

    @Test
    public void testLineageRecordCount() {
        String fileId = "test-file";
        String fileHash = "test-hash";
        Lineage lineage = new Lineage(fileId, fileHash);

        lineage.incrementRecordCount();
        lineage.incrementRecordCount();

        assertEquals(2, lineage.getRecordCount());
    }

    @Test
    public void testLineageRejectedCount() {
        String fileId = "test-file";
        String fileHash = "test-hash";
        Lineage lineage = new Lineage(fileId, fileHash);

        lineage.incrementRejectedCount();
        lineage.incrementRejectedCount();

        assertEquals(2, lineage.getRejectedCount());
    }
}
