# PII Masking — AI Gateway Policy

A Python policy for the WSO2 API Platform AI Gateway that masks
personally identifiable information (PII) in LLM traffic using the OpenMed
deidentification model, running in-process inside the gateway — no sidecar
service required.

## What it does

Before an LLM request leaves the gateway, every PII entity (names, dates, IDs,
and other PHI) detected by the OpenMed model is replaced with a delimited
placeholder such as `<<OPENMED_PHI_NAME_..._000001>>`. The real values never
reach the LLM provider.

On the response, the placeholders the model echoes back are swapped for the
original values, so the client sees a coherent reply while the provider only
ever saw redacted text.

## When to use it

- You send PHI/PII to a third-party LLM (e.g. OpenAI) and need it
  de-identified at the edge, before it leaves your infrastructure.
- You want masking without running a separate de-identification service — the
  model lives inside the gateway runtime.

## How it works

1. **Request flow** — the buffered body is redacted in-process; a
   placeholder → value map is kept in memory for the request.
2. **Response flow** — placeholders are validated and restored. A hallucinated
   or mangled placeholder rejects the restore, and the still-redacted response
   is passed through unchanged rather than leaking data.

Structural fields such as `model` and `role` are left untouched; all free-text
strings, including message and tool content, are redacted before they leave the
gateway.

## Using the policy

Add it to your gateway project's `build.yaml`:

```yaml
version: v1
gateway:
  version: 1.2.1
policies:
  - name: pii-masking
    pipPackage: github.com/wso2/healthcare-accelerator/extensions/policies/ai-gateway/pii-masking@v1
```

Then build the gateway image:

```sh
ap gateway image build --name <gateway-name> --path <gateway-project-dir>
```

Attach `pii-masking` to the LLM provider or route you want masked, the
same way you would any gateway policy.

## Behavior

- Requests must be JSON; non-JSON bodies are rejected with `400`.
- If redaction fails, the request fails closed with `502` — the raw payload is
  never forwarded.
- If the response can't be restored cleanly, the still-redacted response is
  returned instead of leaking the original values.
- The model is warmed up in the background at startup, so the first request
  doesn't pay the full download/load cost (and it's cached across restarts).

## Configuration

The policy accepts a `model` parameter containing any compatible OpenMed model
identifier. If omitted, it uses
`OpenMed/OpenMed-PII-SuperClinical-Small-44M-v1`. The value is passed to
OpenMed when the policy is initialized, so the model can be selected in the
gateway policy YAML without changing Python code. See `policy-definition.yaml`
for the parameter schema.

For example:

```yaml
name: pii-masking
parameters:
  model: OpenMed/OpenMed-PII-ClinicalE5-Small-33M-v1
```

The validated local comparison used these compatible model identifiers:

- `OpenMed/OpenMed-PII-ClinicalE5-Small-33M-v1`
- `OpenMed/OpenMed-PII-LiteClinical-Small-66M-v1`
- `OpenMed/OpenMed-PII-SuperClinical-Small-44M-v1`
- `OpenMed/OpenMed-PII-SuperMedical-Base-125M-v1`

Other OpenMed checkpoints may work when supported by the pinned OpenMed
runtime, but they should be validated locally before deployment.

## Limitations

- The package supports CPython 3.10 and 3.11 on Linux x86_64 so Gateway Builder
  1.2.1 can discover it with Python 3.11. The deployed Gateway 1.2.1 runtime
  remains CPython 3.10.
- Request and response bodies are buffered, so streaming (`stream: true`) isn't
  supported yet.
- Uses the `OpenMed/OpenMed-PII-SuperClinical-Small-44M-v1` model and pins
  the CPU-only `torch==2.13.0` wheel for the model runtime and builder.

## Testing

Install the test extra in a supported Python 3.10 or 3.11 environment:

```sh
python -m pip install -e '.[test]'
pytest -q
```

The tests exercise the policy's redaction and restoration functions directly;
they do not require an AI Gateway or provider process:

```sh
python -m pip install -e '.[test]'
pytest -q
```

## Evaluation notes

On a Python 3.11 CPU run using 100 reproducibly random Bundles from the
September 2019 Synthea FHIR R4 dataset, with a 16 KiB sample per Bundle:

| Model | Reported micro-F1 | Load seconds | Average redaction seconds | Median redaction seconds |
| --- | ---: | ---: | ---: | ---: |
| ClinicalE5-Small | 0.9306 | 11.19 | 7.49 | 7.07 |
| LiteClinical-Small | 0.9485 | 4.89 | 7.52 | 7.26 |
| SuperClinical-Small | 0.9539 | 7.28 | 17.00 | 16.02 |
| SuperMedical-Base | 0.9557 | 4.70 | 19.63 | 18.89 |

These timings measure masking only: model inference, entity extraction,
placeholder replacement, and mapping creation. They do not include response
demasking. The F1 values are reported model-card metrics from the common
Nemotron-PII evaluation, not scores recomputed by this test.

The full source dataset contains 1,180 Bundles and is approximately 1.3 GB.
An attempted complete-payload run of one 717 KB Bundle with 319 FHIR entries
did not finish within approximately six minutes on the same CPU path. The
comparison therefore uses bounded samples and should not be interpreted as
full-payload latency.
