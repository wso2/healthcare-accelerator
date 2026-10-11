# OpenMed PDF redaction sample

These files show OpenMed's text-PDF redaction on a public synthetic medical
record. The source dataset states that all patients and clinical histories are
synthetic and licenses the dataset under CC BY 4.0.

- `synthetic_medical_before.pdf`: the searchable source report from the
  [Synthetic Medical Document Recognition Benchmark](https://huggingface.co/datasets/morzel85/synthetic-medical-document-recognition-benchmark),
  attributed to morzel85 under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).
- `synthetic_medical_after.pdf`: the same report after the new gateway policy's
  OpenMed redaction path.

OpenMed detected 111 PII spans, projected them to 119 page regions, and passed
its PDF text-removal and region verification. The original three pages and page
geometry were preserved. The detector also masked dates and some demographic
or organization-like text beyond the prominent patient identifiers; review and
calibrate entity selection for the intended use before production.

The source dataset's searchable version is used here. Image-only scanned PDFs
need OCR-derived regions and independent re-OCR verification; this policy
rejects them.
