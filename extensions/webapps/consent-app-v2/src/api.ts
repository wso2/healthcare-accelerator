// Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import type { ConsentData, SubmitScopeConsentPayload, SubmitPurposeConsentPayload } from './types';
import { consentBffUrl } from './config';

const BFF_URL = consentBffUrl;

export async function getConsentData(sessionDataKeyConsent: string, spId: string): Promise<ConsentData> {
  const params = new URLSearchParams({ sessionDataKeyConsent, spId });
  const url = BFF_URL ? `${BFF_URL}/v2/get-consent-data?${params}` : `/v2/get-consent-data?${params}`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`BFF error: ${res.status}`);
  return res.json() as Promise<ConsentData>;
}

export async function submitConsent(
  payload: SubmitScopeConsentPayload | SubmitPurposeConsentPayload,
): Promise<void> {
  const url = BFF_URL ? `${BFF_URL}/v2/submit-consent` : '/v2/submit-consent';
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  if (!res.ok) throw new Error(`BFF error: ${res.status}`);
}
