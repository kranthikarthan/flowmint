package ai.flowmint.flink.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * FlowMint execution plan loaded from YAML DSL.
 */
public class Plan {
    
    @JsonProperty("version")
    private Integer version;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("source")
    private SourceConfig source;
    
    @JsonProperty("target")
    private TargetConfig target;
    
    @JsonProperty("mappings")
    private Map<String, String> mappings; // targetField -> expression
    
    @JsonProperty("validations")
    private List<ValidationRule> validations;
    
    @JsonProperty("udfs")
    private List<UdfConfig> udfs;
    
    public Plan() {
    }
    
    public Plan(Integer version, String name, SourceConfig source, TargetConfig target,
                Map<String, String> mappings, List<ValidationRule> validations, List<UdfConfig> udfs) {
        this.version = version;
        this.name = name;
        this.source = source;
        this.target = target;
        this.mappings = mappings;
        this.validations = validations;
        this.udfs = udfs;
    }
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public SourceConfig getSource() {
        return source;
    }
    
    public void setSource(SourceConfig source) {
        this.source = source;
    }
    
    public TargetConfig getTarget() {
        return target;
    }
    
    public void setTarget(TargetConfig target) {
        this.target = target;
    }
    
    public Map<String, String> getMappings() {
        return mappings;
    }
    
    public void setMappings(Map<String, String> mappings) {
        this.mappings = mappings;
    }
    
    public List<ValidationRule> getValidations() {
        return validations;
    }
    
    public void setValidations(List<ValidationRule> validations) {
        this.validations = validations;
    }
    
    public List<UdfConfig> getUdfs() {
        return udfs;
    }
    
    public void setUdfs(List<UdfConfig> udfs) {
        this.udfs = udfs;
    }
    
    @Override
    public String toString() {
        return "Plan{" +
                "version=" + version +
                ", name='" + name + '\'' +
                ", source=" + source +
                ", target=" + target +
                ", mappings=" + mappings +
                ", validations=" + validations +
                ", udfs=" + udfs +
                '}';
    }
}

