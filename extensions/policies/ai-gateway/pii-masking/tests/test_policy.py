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

import importlib.util
import sys
import types
import unittest
from pathlib import Path
from unittest.mock import Mock, patch


def load_policy_module() -> types.ModuleType:
    sdk = types.ModuleType("apip_sdk_core")
    for name in (
        "BodyProcessingMode",
        "DownstreamResponseModifications",
        "ExecutionContext",
        "ImmediateResponse",
        "ProcessingMode",
        "RequestAction",
        "RequestContext",
        "ResponseAction",
        "ResponseContext",
        "UpstreamRequestModifications",
    ):
        setattr(sdk, name, type(name, (), {}))
    sdk.RequestPolicy = type("RequestPolicy", (), {})
    sdk.ResponsePolicy = type("ResponsePolicy", (), {})

    with patch.dict(sys.modules, {"apip_sdk_core": sdk}), patch("threading.Thread"):
        module_path = Path(__file__).parents[1] / "src/pii_masking_v1/policy.py"
        spec = importlib.util.spec_from_file_location("pii_masking_v1.policy", module_path)
        assert spec is not None and spec.loader is not None
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        return module


class RestoreStructureTest(unittest.TestCase):
    def test_restores_placeholder_in_object_key(self) -> None:
        policy_module = load_policy_module()
        policy = policy_module.PiiMaskingPolicy()
        mapping = {"<<OPENMED_PHI_NAME_000001>>": "Jane Doe"}

        with patch.object(policy, "_restore_text", side_effect=lambda text, _: mapping.get(text, text)):
            restored = policy._restore_structure({"<<OPENMED_PHI_NAME_000001>>": "summary"}, mapping)

        self.assertEqual(restored, {"Jane Doe": "summary"})


class PipelineCacheTest(unittest.TestCase):
    def test_reuses_the_loaded_privacy_filter_pipeline(self) -> None:
        policy_module = load_policy_module()
        backend = types.ModuleType("openmed.core.backends")
        backend.create_privacy_filter_pipeline = Mock(return_value=object())

        with patch.dict(
            sys.modules,
            {
                "openmed": types.ModuleType("openmed"),
                "openmed.core": types.ModuleType("openmed.core"),
                "openmed.core.backends": backend,
            },
        ):
            first = policy_module._privacy_filter_pipeline()
            second = policy_module._privacy_filter_pipeline()

        self.assertIs(first, second)
        backend.create_privacy_filter_pipeline.assert_called_once_with(policy_module.MODEL_NAME)


class RequestRedactionScopeTest(unittest.TestCase):
    def test_redacts_request_tool_metadata(self) -> None:
        policy_module = load_policy_module()
        policy = policy_module.PiiMaskingPolicy()
        redacted = []

        def redact_text(value, mapping):
            redacted.append(value)
            return f"redacted:{value}"

        with patch.object(policy, "_redact_text", side_effect=redact_text):
            result = policy._redact_structure(
                {
                    "tools": [
                        {
                            "type": "function",
                            "function": {
                                "name": "read_fhir",
                                "description": "Read records for Jane Doe",
                            },
                        }
                    ],
                    "messages": [
                        {"role": "system", "content": "Static instructions"},
                        {"role": "user", "content": "Jane Doe"},
                    ]
                },
                {},
            )

        self.assertEqual(result["messages"][0]["content"], "Static instructions")
        self.assertEqual(result["messages"][1]["content"], "redacted:Jane Doe")
        self.assertEqual(result["tools"][0]["function"]["description"], "redacted:Read records for Jane Doe")
        self.assertIn("Read records for Jane Doe", redacted)
