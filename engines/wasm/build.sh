#!/usr/bin/env bash
set -e

echo "🔨 Building FlowMint WASM UDFs..."

# Ensure wasm32-wasip1 target is installed
rustup target add wasm32-wasip1 || true

# Build release WASM module
cargo build --target wasm32-wasip1 --release

echo "✅ WASM module built successfully!"
echo "📦 Output: target/wasm32-wasip1/release/flowmint_udfs.wasm"

