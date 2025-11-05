# FlowMint DSL v0.1

YAML describing **source schema**, **field extractions**, **mappings**, **validations**, and **UDFs**.

## Schema

```yaml
version: 1
name: string
source:
  format: xml|csv|json|fixed
  root: xpath/jsonpath/none
  fields:
    fieldName: "expression"
target:
  model: CanonicalPayment
mappings:
  targetField: "expr()"
validations:
  - rule: "boolean-expression"
    code: "SHORT_CODE"
    message: "Human-readable"
udfs:
  - name: hash_sha256
    type: wasm|java
    module: wasm/hash.wasm   # if wasm
    func: hash
    className: com.acme.Hash # if java
```
