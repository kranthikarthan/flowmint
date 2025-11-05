#!/usr/bin/env bash
set -e
echo "✅ Installing FlowMint deps"
if [ ! -d "venv" ]; then
    python3 -m venv venv
fi
python3 -m pip install --target venv/lib/python3.12/site-packages --upgrade pip click rich pyyaml
source ~/.cargo/env 2>/dev/null || true
rustup target add wasm32-wasip1 || true
echo "✅ Bootstrap done"
