# FlowMint WASM UDFs

Rust-based WASM module providing UDF functions for FlowMint pipeline.

## Functions

### `hash_sha256(input: string) -> string`
Computes SHA-256 hash of input string and returns hexadecimal representation.

**Parameters:**
- `input`: String to hash (typically concatenated field values)

**Returns:**
- Hexadecimal string (64 characters) representing SHA-256 hash

### `hash(input: string) -> string`
Alias for `hash_sha256` for compatibility.

### `process(input_json: string) -> string`
Processes JSON input and returns processed JSON output.

**Parameters:**
- `input_json`: JSON string with key-value pairs

**Returns:**
- JSON string with processed data, including:
  - `processed`: boolean indicating success
  - `data`: processed input data (string values uppercased)
  - `timestamp`: processing timestamp

## Build

### Prerequisites
- Rust toolchain (1.70+)
- `wasm32-wasip1` target: `rustup target add wasm32-wasip1`

### Compile to WASM

```bash
cargo build --target wasm32-wasip1 --release
```

The compiled WASM module will be at:
```
target/wasm32-wasip1/release/flowmint_udfs.wasm
```

### Build Script

```bash
./build.sh
```

Or using Make:
```bash
make build
```

## Usage in Flink

The WASM module is loaded by `WasmtimeExecutor` in the Flink pipeline:

```yaml
udfs:
  - name: hash_sha256
    type: wasm
    module: wasm/hash.wasm
    func: hash_sha256
```

## Testing

```bash
cargo test
```

## Memory Management

The WASM module exports a `free_string` function that should be called to release
memory allocated by exported functions. The Java WasmtimeExecutor handles this automatically.
