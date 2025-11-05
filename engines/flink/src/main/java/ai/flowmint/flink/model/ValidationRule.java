package ai.flowmint.flink.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Validation rule with code and message.
 */
public class ValidationRule {
    
    @JsonProperty("rule")
    private String rule; // boolean expression
    
    @JsonProperty("code")
    private String code; // short code like "AMT_POS"
    
    @JsonProperty("message")
    private String message; // human-readable message
    
    public ValidationRule() {
    }
    
    public ValidationRule(String rule, String code, String message) {
        this.rule = rule;
        this.code = code;
        this.message = message;
    }
    
    public String getRule() {
        return rule;
    }
    
    public void setRule(String rule) {
        this.rule = rule;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "ValidationRule{" +
                "rule='" + rule + '\'' +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}

