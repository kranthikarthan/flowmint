#!/usr/bin/env python3
import sys, json, yaml, click
from pathlib import Path

@click.group()
def cli():
    "FlowMint CLI — validate mappings, plan pipelines, run local demos."

@cli.command()
@click.argument("mapping", type=click.Path(exists=True))
def validate(mapping):
    "Validate a mapping YAML (schema sanity checks)."
    with open(mapping, "r", encoding="utf-8") as f:
        y = yaml.safe_load(f)
    
    # Required top-level keys
    required = ["version", "name", "source", "target", "mappings"]
    missing = [k for k in required if k not in y]
    if missing:
        click.echo(f"❌ Missing required keys: {missing}")
        sys.exit(1)
    
    # Validate source structure
    source = y.get("source", {})
    if not isinstance(source, dict):
        click.echo("❌ 'source' must be an object")
        sys.exit(1)
    if "format" not in source:
        click.echo("❌ 'source.format' is required")
        sys.exit(1)
    
    # Validate target structure
    target = y.get("target", {})
    if not isinstance(target, dict):
        click.echo("❌ 'target' must be an object")
        sys.exit(1)
    if "model" not in target:
        click.echo("❌ 'target.model' is required")
        sys.exit(1)
    
    # Validate mappings
    mappings = y.get("mappings", {})
    if not isinstance(mappings, dict):
        click.echo("❌ 'mappings' must be an object")
        sys.exit(1)
    
    click.echo("✅ Mapping file looks valid")

@cli.command("plan")
@click.argument("mapping", type=click.Path(exists=True))
def plan(mapping):
    "Print a simple execution plan from mapping YAML."
    y = yaml.safe_load(open(mapping,"r",encoding="utf-8"))
    src = y.get("source",{}).get("format")
    fields = list((y.get("source",{}).get("fields") or {}).keys())
    maps = list((y.get("mappings") or {}).keys())
    click.echo(json.dumps({
        "sourceFormat": src,
        "extractFields": fields,
        "targetFields": maps,
    }, indent=2))

@cli.command("run-local")
@click.argument("mapping", type=click.Path(exists=True))
@click.argument("input_path", type=click.Path(exists=True))
def run_local(mapping, input_path):
    "Demo: echo records count (placeholder for local run)."
    p = Path(input_path)
    total = 0
    if p.is_file():
        total = sum(1 for _ in open(p, "rb"))
    else:
        for fp in p.rglob("*"):
            if fp.is_file():
                total += sum(1 for _ in open(fp, "rb"))
    click.echo(f"🟢 Processed {total} lines (demo).")

if __name__ == "__main__":
    cli()
