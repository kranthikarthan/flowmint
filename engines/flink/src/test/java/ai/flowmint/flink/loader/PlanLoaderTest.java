package ai.flowmint.flink.loader;

import ai.flowmint.flink.model.Plan;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PlanLoader.
 */
class PlanLoaderTest {
    
    @Test
    void testLoadPlanFromFile() throws Exception {
        // This test would require a sample YAML file
        // For now, we'll test the validation logic
        PlanLoader loader = new PlanLoader();
        
        // Test null plan validation
        assertThrows(IllegalArgumentException.class, () -> loader.validate(null));
    }
    
    @Test
    void testValidatePlan() {
        PlanLoader loader = new PlanLoader();
        Plan plan = new Plan();
        
        // Test missing version
        assertThrows(IllegalArgumentException.class, () -> loader.validate(plan));
        
        plan.setVersion(1);
        // Test missing name
        assertThrows(IllegalArgumentException.class, () -> loader.validate(plan));
        
        plan.setName("test");
        // Test missing source
        assertThrows(IllegalArgumentException.class, () -> loader.validate(plan));
    }
}

