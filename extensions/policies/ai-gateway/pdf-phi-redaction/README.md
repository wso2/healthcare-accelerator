# PDF PHI Redaction — AI Gateway Policy

This policy redacts **inline base64 PDF attachments inside buffered JSON LLM
requests** with OpenMed before forwarding the request upstream. It is separate
from `pii-masking`, which only processes JSON text strings.

## Request contract

The policy recursively finds inline file parts shaped like either of these
forms:

```json
{"type":"file","file":{"filename":"record.pdf","file_data":"data:application/pdf;base64,<base64 PDF>"}}
{"type":"input_file","filename":"record.pdf","file_data":"data:application/pdf;base64,<base64 PDF>"}
```

Raw standard base64 is accepted when it decodes to a PDF. This maps to provider
APIs that carry file content inline in JSON. It does not support multipart HTTP
bodies, provider file IDs, remote URLs, non-JSON bodies, or non-PDF attachments.
Requests without a supported inline PDF are rejected so a file reference
cannot silently bypass the policy.

The policy only redacts the PDF attachment; it does not redact other prompt
text in the same JSON request. The existing `pii-masking` policy currently
walks every JSON string, so do not chain it with this policy until its handler
is updated to skip encoded PDF content.

## Processing

1. Decode and validate the PDF signature and configured byte limit.
2. Extract digital PDF text with OpenMed, run the configured PII model, and map
   detected character spans to page rectangles.
3. Render a new PDF with opaque redactions and OpenMed's text-removal and layout
   verification. Only after verification passes, replace `file_data` with the
   newly rendered PDF and forward the JSON body.
4. If parsing, detection, span projection, or verification fails, reject the
   request. If extraction succeeds but the detector finds no PII spans, the
   validated PDF passes through unchanged. Raw PDF bytes and detected text are
   never logged.

The PDF renderer uses local temporary files because the documented path-based
OpenMed API accepts file paths. Temporary files are removed in a `finally`
block. Deploy on encrypted ephemeral storage and restrict the gateway process
filesystem; use an in-memory preprocessor service if disk writes are not
acceptable.

## Limits and boundaries

- Digital, text-bearing PDFs only. Scanned or image-only PDFs are rejected;
  OCR plus re-OCR verification is not implemented in this policy.
- Each PDF is limited by `maxPdfBytes` (default 10 MiB, maximum 25 MiB) and
  `maxPages` (default 20, maximum 100).
- PDFs with no extractable text are rejected. A successfully extracted PDF
  with no detected PII is forwarded unchanged.
- Existing source metadata, annotations, forms, embedded files, and thumbnails
  are not preserved by OpenMed's verified renderer.
- Request bodies are buffered. Streaming and response-side placeholder
  restoration are not supported.
- The PII model is loaded once per model identifier in each gateway process.
  Preload the model artifacts or provide the gateway process with Hugging Face
  access before enabling the route.
- This policy needs OpenMed 2.5+ and its `multimodal` dependencies, independent
  of the existing text policy's OpenMed 1.x pin. Check gateway Python runtime,
  image size, model cache, PDFium compatibility, timeouts, and memory limits in
  the target deployment.

## Configuration

| Parameter | Default | Purpose |
| --- | --- | --- |
| `model` | `OpenMed/OpenMed-PII-SuperClinical-Small-44M-v1` | OpenMed PII model |
| `maxPdfBytes` | `10485760` | Maximum decoded bytes per PDF |
| `maxPages` | `20` | Maximum pages per PDF |

## Add to a gateway build

```yaml
policies:
  - name: pdf-phi-redaction
    pipPackage: github.com/wso2/healthcare-accelerator/extensions/policies/ai-gateway/pdf-phi-redaction@v1
```

Attach this policy to the AI route/provider that receives the inline PDF JSON.
The provider request format must preserve the `file_data` field after the
policy modifies it.
