use serde_json::json;
use sha2::{Sha256, Digest};
use std::ffi::{CStr, CString};
use std::os::raw::c_char;

/// Hash SHA-256 function that takes a concatenated string and returns hex string
/// 
/// This function is exported as a C function for WASM compatibility.
/// Input: null-terminated C string containing concatenated parameters
/// Output: null-terminated C string containing hex-encoded SHA-256 hash
#[no_mangle]
pub extern "C" fn hash_sha256(input_ptr: *const c_char) -> *mut c_char {
    let input = unsafe {
        if input_ptr.is_null() {
            return CString::new("").unwrap().into_raw();
        }
        match CStr::from_ptr(input_ptr).to_str() {
            Ok(s) => s,
            Err(_) => return CString::new("").unwrap().into_raw(),
        }
    };
    
    let mut hasher = Sha256::new();
    hasher.update(input.as_bytes());
    let result = hasher.finalize();
    let hex_string = format!("{:x}", result);
    
    CString::new(hex_string).unwrap_or_default().into_raw()
}

/// Hash function (alias for hash_sha256 for compatibility)
#[no_mangle]
pub extern "C" fn hash(input_ptr: *const c_char) -> *mut c_char {
    hash_sha256(input_ptr)
}

/// Process function that takes JSON input and returns processed JSON
/// 
/// Input: null-terminated C string containing JSON
/// Output: null-terminated C string containing processed JSON
#[no_mangle]
pub extern "C" fn process(input_ptr: *const c_char) -> *mut c_char {
    let input_json = unsafe {
        if input_ptr.is_null() {
            return CString::new(json!({"error": "null input"}).to_string()).unwrap().into_raw();
        }
        match CStr::from_ptr(input_ptr).to_str() {
            Ok(s) => s,
            Err(_) => return CString::new(json!({"error": "invalid utf-8"}).to_string()).unwrap().into_raw(),
        }
    };
    
    // Parse input JSON
    let input: serde_json::Value = match serde_json::from_str(input_json) {
        Ok(v) => v,
        Err(e) => {
            let error_json = json!({
                "error": "Invalid JSON input",
                "message": e.to_string()
            });
            return CString::new(error_json.to_string()).unwrap_or_default().into_raw();
        }
    };
    
    // Process the input (example: uppercase string values)
    let mut output = serde_json::Map::new();
    output.insert("processed".to_string(), json!(true));
    
    // Copy and transform input fields
    if let Some(obj) = input.as_object() {
        let mut processed_data = serde_json::Map::new();
        for (key, value) in obj {
            if let Some(str_val) = value.as_str() {
                processed_data.insert(key.clone(), json!(str_val.to_uppercase()));
            } else {
                processed_data.insert(key.clone(), value.clone());
            }
        }
        output.insert("data".to_string(), json!(processed_data));
    } else {
        output.insert("input".to_string(), input);
    }
    
    // Return as JSON string
    let result_json = json!(output).to_string();
    CString::new(result_json).unwrap_or_default().into_raw()
}

/// Free function to release memory allocated by hash_sha256, hash, or process
#[no_mangle]
pub extern "C" fn free_string(ptr: *mut c_char) {
    if !ptr.is_null() {
        unsafe {
            let _ = CString::from_raw(ptr);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::ffi::{CString, CStr};

    #[test]
    fn test_hash_sha256() {
        let input = CString::new("test").unwrap();
        let result_ptr = hash_sha256(input.as_ptr());
        let result = unsafe {
            CStr::from_ptr(result_ptr).to_str().unwrap().to_string()
        };
        free_string(result_ptr);
        assert_eq!(result.len(), 64); // SHA-256 produces 64 hex characters
    }

    #[test]
    fn test_process() {
        let input = CString::new(r#"{"name": "test", "value": "hello"}"#).unwrap();
        let result_ptr = process(input.as_ptr());
        let result = unsafe {
            CStr::from_ptr(result_ptr).to_str().unwrap().to_string()
        };
        free_string(result_ptr);
        assert!(result.contains("processed"));
    }
}
