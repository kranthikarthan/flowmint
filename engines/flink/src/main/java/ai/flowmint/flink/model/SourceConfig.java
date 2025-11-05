package ai.flowmint.flink.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Configuration for source data format and field extraction.
 */
public class SourceConfig {
    
    @JsonProperty("format")
    private String format; // xml, csv, json, fixed
    
    @JsonProperty("root")
    private String root; // xpath/jsonpath expression
    
    @JsonProperty("fields")
    private Map<String, String> fields; // fieldName -> expression
    
    public SourceConfig() {
    }
    
    public SourceConfig(String format, String root, Map<String, String> fields) {
        this.format = format;
        this.root = root;
        this.fields = fields;
    }
    
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    public String getRoot() {
        return root;
    }
    
    public void setRoot(String root) {
        this.root = root;
    }
    
    public Map<String, String> getFields() {
        return fields;
    }
    
    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }
    
    @Override
    public String toString() {
        return "SourceConfig{" +
                "format='" + format + '\'' +
                ", root='" + root + '\'' +
                ", fields=" + fields +
                '}';
    }
}

