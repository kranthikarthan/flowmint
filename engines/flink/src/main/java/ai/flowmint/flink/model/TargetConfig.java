package ai.flowmint.flink.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for target canonical model.
 */
public class TargetConfig {
    
    @JsonProperty("model")
    private String model; // e.g., "CanonicalPayment"
    
    public TargetConfig() {
    }
    
    public TargetConfig(String model) {
        this.model = model;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    @Override
    public String toString() {
        return "TargetConfig{" +
                "model='" + model + '\'' +
                '}';
    }
}

