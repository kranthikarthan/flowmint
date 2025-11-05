package ai.flowmint.flink.loader;

import ai.flowmint.flink.model.Plan;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Loads FlowMint DSL YAML files and converts them to Plan POJOs.
 */
public class PlanLoader {
    
    private static final Logger LOG = LoggerFactory.getLogger(PlanLoader.class);
    
    private final ObjectMapper yamlMapper;
    
    public PlanLoader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }
    
    /**
     * Load a plan from a YAML file path.
     *
     * @param yamlPath path to YAML file
     * @return parsed Plan
     * @throws IOException if file cannot be read or parsed
     */
    public Plan loadFromFile(String yamlPath) throws IOException {
        LOG.info("Loading plan from file: {}", yamlPath);
        File file = new File(yamlPath);
        if (!file.exists()) {
            throw new IOException("Plan file not found: " + yamlPath);
        }
        return yamlMapper.readValue(file, Plan.class);
    }
    
    /**
     * Load a plan from a Path.
     *
     * @param yamlPath path to YAML file
     * @return parsed Plan
     * @throws IOException if file cannot be read or parsed
     */
    public Plan loadFromPath(Path yamlPath) throws IOException {
        LOG.info("Loading plan from path: {}", yamlPath);
        return yamlMapper.readValue(yamlPath.toFile(), Plan.class);
    }
    
    /**
     * Load a plan from an InputStream.
     *
     * @param inputStream input stream containing YAML
     * @return parsed Plan
     * @throws IOException if stream cannot be read or parsed
     */
    public Plan loadFromStream(InputStream inputStream) throws IOException {
        LOG.info("Loading plan from input stream");
        return yamlMapper.readValue(inputStream, Plan.class);
    }
    
    /**
     * Validate that a Plan has required fields.
     *
     * @param plan plan to validate
     * @throws IllegalArgumentException if plan is invalid
     */
    public void validate(Plan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("Plan cannot be null");
        }
        if (plan.getVersion() == null) {
            throw new IllegalArgumentException("Plan version is required");
        }
        if (plan.getName() == null || plan.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Plan name is required");
        }
        if (plan.getSource() == null) {
            throw new IllegalArgumentException("Plan source configuration is required");
        }
        if (plan.getSource().getFormat() == null || plan.getSource().getFormat().trim().isEmpty()) {
            throw new IllegalArgumentException("Plan source format is required");
        }
        if (plan.getTarget() == null) {
            throw new IllegalArgumentException("Plan target configuration is required");
        }
        LOG.debug("Plan validation passed: {}", plan.getName());
    }
}

