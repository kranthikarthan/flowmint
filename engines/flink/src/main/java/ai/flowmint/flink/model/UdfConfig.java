package ai.flowmint.flink.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * UDF configuration for WASM or Java functions.
 */
public class UdfConfig {
    
    @JsonProperty("name")
    private String name; // function name
    
    @JsonProperty("type")
    private String type; // "wasm" or "java"
    
    @JsonProperty("module")
    private String module; // path to .wasm file (if wasm)
    
    @JsonProperty("func")
    private String func; // function name in wasm module
    
    @JsonProperty("className")
    private String className; // Java class name (if java)
    
    public UdfConfig() {
    }
    
    public UdfConfig(String name, String type, String module, String func, String className) {
        this.name = name;
        this.type = type;
        this.module = module;
        this.func = func;
        this.className = className;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getModule() {
        return module;
    }
    
    public void setModule(String module) {
        this.module = module;
    }
    
    public String getFunc() {
        return func;
    }
    
    public void setFunc(String func) {
        this.func = func;
    }
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public boolean isWasm() {
        return "wasm".equalsIgnoreCase(type);
    }
    
    public boolean isJava() {
        return "java".equalsIgnoreCase(type);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UdfConfig udfConfig = (UdfConfig) o;
        return Objects.equals(name, udfConfig.name) &&
                Objects.equals(type, udfConfig.type) &&
                Objects.equals(module, udfConfig.module) &&
                Objects.equals(func, udfConfig.func) &&
                Objects.equals(className, udfConfig.className);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, type, module, func, className);
    }
    
    @Override
    public String toString() {
        return "UdfConfig{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", module='" + module + '\'' +
                ", func='" + func + '\'' +
                ", className='" + className + '\'' +
                '}';
    }
}

