# Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
#
# WSO2 LLC. licenses this file to you under the Apache License,
# Version 2.0 (the "License"); you may not use this file except
# in compliance with the License. You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from __future__ import annotations

import base64
import binascii
import json
import logging
import re
import threading
from io import BytesIO
from pathlib import Path
from typing import Any

from apip_sdk_core import (
    BodyProcessingMode,
    ExecutionContext,
    ImmediateResponse,
    ProcessingMode,
    RequestAction,
    RequestContext,
    RequestPolicy,
    UpstreamRequestModifications,
)

DEFAULT_MODEL = "OpenMed/OpenMed-PII-SuperClinical-Small-44M-v1"
DEFAULT_MAX_PDF_BYTES = 10 * 1024 * 1024
DEFAULT_MAX_PAGES = 20
_PDF_MAGIC = b"%PDF-"
_DATA_URI_PREFIX = "data:application/pdf;base64,"
_STANDARD_B64 = re.compile(r"^[A-Za-z0-9+/]*={0,2}$")
_MODEL_LOADERS: dict[str, Any] = {}
_MODEL_LOCK = threading.Lock()

logger = logging.getLogger("pdf-phi-redaction")
logger.setLevel(logging.INFO)
logger.propagate = False
if not logger.handlers:
    handler = logging.StreamHandler()
    handler.setFormatter(logging.Formatter("%(message)s"))
    logger.addHandler(handler)


class PdfPolicyError(ValueError):
    """An input PDF cannot be safely processed by this policy."""


def _extract_pii(text: str, model_name: str) -> Any:
    from openmed import extract_pii
    from openmed.core.models import ModelLoader

    if model_name not in _MODEL_LOADERS:
        with _MODEL_LOCK:
            if model_name not in _MODEL_LOADERS:
                _MODEL_LOADERS[model_name] = ModelLoader()
    return extract_pii(text, model_name=model_name, loader=_MODEL_LOADERS[model_name])


def _error(status: int, message: str) -> ImmediateResponse:
    return ImmediateResponse(
        status_code=status,
        body=json.dumps({"error": message}, separators=(",", ":")).encode(),
        headers={"content-type": "application/json"},
    )


def _decode_pdf(value: str, max_pdf_bytes: int) -> tuple[bytes, bool]:
    prefix = value.startswith(_DATA_URI_PREFIX)
    encoded = value[len(_DATA_URI_PREFIX):] if prefix else value
    if len(encoded) > ((max_pdf_bytes + 2) // 3) * 4 + 4:
        raise PdfPolicyError("inline PDF exceeds the configured size limit")
    if not encoded or len(encoded) % 4 or not _STANDARD_B64.fullmatch(encoded):
        raise PdfPolicyError("PDF file_data must contain standard base64")
    try:
        content = base64.b64decode(encoded, validate=True)
    except (binascii.Error, ValueError) as exc:
        raise PdfPolicyError("PDF file_data is invalid base64") from exc
    if len(content) > max_pdf_bytes:
        raise PdfPolicyError("inline PDF exceeds the configured size limit")
    if not content.startswith(_PDF_MAGIC):
        raise PdfPolicyError("decoded file_data is not a PDF")
    return content, prefix


def _find_file_data(node: Any, locations: list[tuple[dict[str, Any], str]]) -> None:
    if isinstance(node, dict):
        part_type = node.get("type")
        if part_type in {"file", "input_file"}:
            file_object = node.get("file") if isinstance(node.get("file"), dict) else node
            if "file_data" not in file_object:
                raise PdfPolicyError("PDF file parts must provide inline file_data; file IDs and URLs are unsupported")
            value = file_object["file_data"]
            if not isinstance(value, str):
                raise PdfPolicyError("PDF file_data must be a base64 string")
            mime_type = file_object.get("mime_type") or file_object.get("media_type")
            if mime_type is not None and mime_type != "application/pdf":
                raise PdfPolicyError("only application/pdf file parts are supported")
            locations.append((file_object, value))
            return
        for value in node.values():
            _find_file_data(value, locations)
    elif isinstance(node, list):
        for value in node:
            _find_file_data(value, locations)


def _redact_pdf(content: bytes, model_name: str, max_pages: int) -> bytes:
    import tempfile

    from openmed.multimodal import extract_pdf, project_text_spans, render_redacted_pdf

    source_path: Path | None = None
    output_path: Path | None = None
    try:
        with tempfile.NamedTemporaryFile(suffix=".pdf", delete=False) as source:
            source.write(content)
            source_path = Path(source.name)
        with tempfile.NamedTemporaryFile(suffix=".pdf", delete=False) as output:
            output_path = Path(output.name)
        output_path.unlink()

        import pdfplumber

        with pdfplumber.open(BytesIO(content)) as pdf:
            page_count = len(pdf.pages)
        if page_count < 1:
            raise PdfPolicyError("PDF contains no pages")
        if page_count > max_pages:
            raise PdfPolicyError("PDF exceeds the configured page limit")

        document = extract_pdf(source_path)
        if not document.text.strip():
            raise PdfPolicyError("PDF has no extractable text; scanned PDFs are unsupported")
        prediction = _extract_pii(document.text, model_name)
        spans = [
            (entity.start, entity.end)
            for entity in getattr(prediction, "entities", [])
            if isinstance(getattr(entity, "start", None), int)
            and isinstance(getattr(entity, "end", None), int)
            and entity.start < entity.end
        ]
        if not spans:
            return content

        regions = project_text_spans(document, spans)
        if not regions:
            raise PdfPolicyError("detected PII could not be mapped to PDF page regions")
        result = render_redacted_pdf(
            source_path,
            output_path,
            regions,
            max_pages=max_pages,
        )
        if not getattr(result, "passed", False) or not output_path.is_file():
            raise PdfPolicyError("PDF redaction verification did not pass")
        return output_path.read_bytes()
    finally:
        if source_path is not None:
            source_path.unlink(missing_ok=True)
        if output_path is not None:
            output_path.unlink(missing_ok=True)


def _redact_request(payload: Any, params: dict[str, Any]) -> tuple[Any, int]:
    model_name = params.get("model", DEFAULT_MODEL)
    max_pdf_bytes = params.get("maxPdfBytes", DEFAULT_MAX_PDF_BYTES)
    max_pages = params.get("maxPages", DEFAULT_MAX_PAGES)
    if not isinstance(model_name, str) or not model_name.strip():
        raise PdfPolicyError("model must be a non-empty string")
    if type(max_pdf_bytes) is not int or not 1 <= max_pdf_bytes <= 25 * 1024 * 1024:
        raise PdfPolicyError("maxPdfBytes must be between 1 and 26214400")
    if type(max_pages) is not int or not 1 <= max_pages <= 100:
        raise PdfPolicyError("maxPages must be between 1 and 100")

    locations: list[tuple[dict[str, Any], str]] = []
    _find_file_data(payload, locations)
    if not locations:
        raise PdfPolicyError("request contains no supported inline PDF file_data")

    for node, value in locations:
        pdf, data_uri = _decode_pdf(value, max_pdf_bytes)
        redacted = _redact_pdf(pdf, model_name, max_pages)
        encoded = base64.b64encode(redacted).decode("ascii")
        node["file_data"] = f"{_DATA_URI_PREFIX}{encoded}" if data_uri else encoded
        if "mime_type" in node:
            node["mime_type"] = "application/pdf"
        if "media_type" in node:
            node["media_type"] = "application/pdf"
    return payload, len(locations)


class PdfPhiRedactionPolicy(RequestPolicy):
    """Redact inline base64 PDF parts in a buffered JSON request."""

    def mode(self) -> ProcessingMode:
        return ProcessingMode(request_body_mode=BodyProcessingMode.BUFFER)

    def on_request_body(
        self,
        execution_ctx: ExecutionContext,
        ctx: RequestContext,
        params: dict[str, Any],
    ) -> RequestAction:
        if ctx.body is None or not ctx.body.present or ctx.body.content is None:
            return _error(400, "request body is required")
        try:
            payload = json.loads(ctx.body.content)
        except (json.JSONDecodeError, UnicodeDecodeError):
            return _error(400, "request body must be JSON containing inline PDF file_data")

        try:
            redacted, count = _redact_request(payload, params or {})
            body = json.dumps(redacted, separators=(",", ":")).encode()
        except PdfPolicyError as exc:
            logger.info("request %s: PDF redaction rejected: %s", ctx.shared.request_id, exc)
            return _error(422, str(exc))
        except Exception:
            # Never forward the request when detection or PDF verification failed.
            logger.error("request %s: PDF redaction failed", ctx.shared.request_id)
            return _error(502, "PDF PHI redaction failed")

        logger.info("request %s: verified %d inline PDF(s) for forwarding", ctx.shared.request_id, count)
        return UpstreamRequestModifications(
            body=body,
            headers_to_set={"content-length": str(len(body))},
        )


def get_policy(metadata, params):
    return PdfPhiRedactionPolicy()
