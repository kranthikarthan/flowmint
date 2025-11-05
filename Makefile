bootstrap:
	@bash -c 'if [ ! -d "venv" ]; then python3 -m venv venv; fi && \
		python3 -m pip install --target venv/lib/python3.12/site-packages --upgrade pip click rich pyyaml && \
		(source ~/.cargo/env 2>/dev/null || true) && \
		rustup target add wasm32-wasip1 || true && \
		echo "✅ FlowMint dev environment ready"'

lint:
	@if [ -d "venv" ]; then \
		venv/bin/python3 -m pyflakes cli/*.py || true; \
	else \
		python3 -m pyflakes cli/*.py || true; \
	fi

run-cli-validate:
	@if [ -d "venv" ]; then \
		venv/bin/python3 cli/flowmintctl.py validate dsl/examples/iso20022-pain001.yaml; \
	else \
		python3 cli/flowmintctl.py validate dsl/examples/iso20022-pain001.yaml; \
	fi

run-cli-plan:
	@if [ -d "venv" ]; then \
		venv/bin/python3 cli/flowmintctl.py plan dsl/examples/iso20022-pain001.yaml; \
	else \
		python3 cli/flowmintctl.py plan dsl/examples/iso20022-pain001.yaml; \
	fi

run-cli-demo:
	@if [ -d "venv" ]; then \
		venv/bin/python3 cli/flowmintctl.py run-local dsl/examples/iso20022-pain001.yaml dsl/examples; \
	else \
		python3 cli/flowmintctl.py run-local dsl/examples/iso20022-pain001.yaml dsl/examples; \
	fi
