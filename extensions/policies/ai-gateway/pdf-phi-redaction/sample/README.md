# Sample PDFs

`synthetic_medical_before.pdf` is a synthetic medical report from the
[Synthetic Medical Document Recognition Benchmark](https://huggingface.co/datasets/morzel85/synthetic-medical-document-recognition-benchmark),
shared by morzel85 under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).
`synthetic_medical_after.pdf` shows the report after OpenMed redaction.

OpenMed detected 111 PII spans and redacted 119 page regions. Verification
passed, and the three-page layout was preserved. Some dates and demographic or
organization-like text were also masked. Scanned PDFs are not supported.
