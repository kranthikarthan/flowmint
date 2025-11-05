package ai.flowmint.flink.wasm;

import ai.flowmint.flink.model.UdfConfig;

import java.util.Map;

/**
 * Interface for executing WASM-based UDFs.
 * 
 * This interface provides an abstraction for WASM execution, allowing
 * integration with different WASM runtimes (e.g., Wasmtime, Wasmer).
 */
public interface WasmExecutor {
    
    /**
     * Initialize a WASM module from configuration.
     *
     * @param udfConfig UDF configuration containing module path and function name
     * @throws WasmExecutionException if module cannot be loaded or initialized
     */
    void initialize(UdfConfig udfConfig) throws WasmExecutionException;
    
    /**
     * Execute a WASM function with given parameters.
     *
     * @param functionName name of the function to call
     * @param parameters map of parameter names to values
     * @return result as a string (typically JSON)
     * @throws WasmExecutionException if execution fails
     */
    String execute(String functionName, Map<String, String> parameters) throws WasmExecutionException;
    
    /**
     * Check if a module is loaded and ready.
     *
     * @return true if initialized and ready
     */
    boolean isInitialized();
    
    /**
     * Clean up resources (close module, release memory).
     */
    void close();
    
    /**
     * Exception thrown when WASM execution fails.
     */
    class WasmExecutionException extends Exception {
        public WasmExecutionException(String message) {
            super(message);
        }
        
        public WasmExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

