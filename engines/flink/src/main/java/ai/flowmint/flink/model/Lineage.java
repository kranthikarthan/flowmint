package ai.flowmint.flink.model;

import java.time.Instant;

public class Lineage {

    private String fileId;
    private String fileHash;
    private Instant ingestTime;
    private Instant completionTime;
    private long recordCount;
    private long rejectedCount;
    private String mappingVersion;
    private String udfVersionHash;

    public Lineage(String fileId, String fileHash) {
        this.fileId = fileId;
        this.fileHash = fileHash;
        this.ingestTime = Instant.now();
    }

    // Getters and setters

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public Instant getIngestTime() {
        return ingestTime;
    }

    public void setIngestTime(Instant ingestTime) {
        this.ingestTime = ingestTime;
    }

    public Instant getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(Instant completionTime) {
        this.completionTime = completionTime;
    }

    public long getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(long recordCount) {
        this.recordCount = recordCount;
    }

    public void incrementRecordCount() {
        this.recordCount++;
    }

    public long getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(long rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public void incrementRejectedCount() {
        this.rejectedCount++;
    }

    public String getMappingVersion() {
        return mappingVersion;
    }

    public void setMappingVersion(String mappingVersion) {
        this.mappingVersion = mappingVersion;
    }

    public String getUdfVersionHash() {
        return udfVersionHash;
    }

    public void setUdfVersionHash(String udfVersionHash) {
        this.udfVersionHash = udfVersionHash;
    }

    public void markCompleted() {
        this.completionTime = Instant.now();
    }
}
