// Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

// ─── Config types ────────────────────────────────────────────────────────────

type ScopeConsentConfig record {|
    string purposeName = "SMART Scope Authorization";
|};

// Only the purpose name is configured here — description, mandatory flag, and
// elements are fetched from WSO2 IS at startup via the consent management API.
type PurposeConsentConfig record {|
    string purposeName;
|};

// ─── IS consent API: purpose response types ─────────────────────────────────
// Open records: extra fields returned by WSO2 IS are silently ignored.

type IsPurposeSummary record {
    string? id = ();
    string name;
};

type IsPurposeListResponse record {
    IsPurposeSummary[] Purposes = [];
};

type IsPurposeElement record {
    string? id = ();
    string name;
    string? displayName = ();
    boolean mandatory = false;
};

type IsPurpose record {
    string? id = ();
    string name;
    string? description = ();
    IsPurposeElement[] elements = [];
};

// In-memory representation of a purpose fetched from WSO2 IS at startup.
// readonly so it can be transferred into/out of isolated lock blocks without cloning.
type CachedPurpose readonly & record {|
    string id;
    string name;
    string description?;
    string[] elementNames;
    // element name -> element id (consents bind elements by id)
    map<string> elementIds;
    boolean anyMandatory;
|};

// ─── Shared response types ────────────────────────────────────────────────────

type ConsentUser record {|
    string id;
    string displayName;
    string email;
|};

type ConsentPatient record {|
    string id;
    string name;
    string fhirUser;
    string mrn?;
|};

type ConsentPurpose record {|
    string purposeName;
    boolean mandatory;
    string purposeDescription?;
    string[] elements;
|};

// ─── get-consent-data response shapes ────────────────────────────────────────

type RedirectConsentData record {|
    string flow = "redirect";
    string redirectUrl;
|};

type ScopeConsentData record {|
    string flow = "scope";
    string sessionDataKeyConsent;
    string spId;
    ConsentUser user;
    boolean isPractitioner;
    ConsentPatient[] patients?;
    string[] scopes;
    string[] hiddenScopes;
    string mandatoryClaims;
    string existingConsentId?;
    string[] previouslyApprovedScopes?;
    string consentExpiryOption?;
    string consentToken;
|};

type PurposeConsentData record {|
    string flow = "purpose";
    string sessionDataKeyConsent;
    string spId;
    string appName;
    ConsentUser user;
    ConsentPurpose[] purposes;
    string[] scopes;
    string mandatoryClaims;
    string existingConsentId?;
    string[] previouslyConsentedPurposeNames?;
    map<string[]> previouslyConsentedElements?;
    string consentToken;
|};

// ─── submit-consent request/response ─────────────────────────────────────────

type ConsentedPurpose record {|
    string purposeName;
    string[] consentedElements;
|};

type SubmitConsentRequest record {|
    string consentToken;
    string sessionDataKeyConsent;
    string spId;
    boolean approved;
    string[] approvedScopes?;
    string[] hiddenScopes?;
    ConsentedPurpose[] consentedPurposes?;
    string existingConsentId?;
    string consentExpiryOption?;
|};

type SubmitConsentResponse record {|
    string status;
    string message;
|};

// ─── EHR launch context ───────────────────────────────────────────────────────

type EhrLaunchContext record {|
    string launchId;
    string patientId?;
    string encounterId?;
    string aud;
    string expiry;
|};

// ─── IDP / OauthConsentKey response ──────────────────────────────────────────

// Open record — IDP may return additional fields (mandatoryClaims, etc.)
type IdpConsentKeyResponse record {
    string application;
    string scope;
    string loggedInUser;
    string mandatoryClaims?;
    string spQueryParams?;
};

// Internal type returned by getScimUser — includes fhirUser for isPractitioner check.
type ScimUserInfo record {|
    string id;
    string displayName;
    string email;
    string fhirUser;
|};

// ─── Internal type for existing consent lookup ────────────────────────────────

type ExistingConsentData record {|
    string consentId;
    int validityTime?;
    string consentExpiryOption?;
    string[] approvedScopes;           // scope flow
    string[] consentedPurposeNames;    // purpose flow
    map<string[]> consentedElements;   // purpose flow
|};

// ─── IS consent API: consent payload types ───────────────────────────────────

type IsConsentElementRef record {|
    string id;
|};

type IsConsentPurposeBinding record {|
    string id;
    IsConsentElementRef[] elements;
|};

type IsConsentCreateRequest record {|
    string subjectId;
    string serviceId;
    // IS 7.3.0 stores the language in a NOT NULL column, so it must always be sent
    string language = "en";
    IsConsentPurposeBinding[] purposes;
    // Milliseconds since epoch; omitted for consents that never expire
    int expiryTime?;
    map<string> properties?;
|};

type IsConsentCreatedResponse record {
    string id;
};

// ─── IS consent API: consent response types ──────────────────────────────────

type IsConsentSummary record {
    string id;
    string? state = ();
};

type IsConsentListResponse record {
    IsConsentSummary[] Consents = [];
};

type IsConsentedElement record {
    string name;
};

type IsConsentedPurpose record {
    string name;
    IsConsentedElement[] elements = [];
};

type IsConsentDetail record {
    string id;
    int? expiryTime = ();
    IsConsentedPurpose[] purposes = [];
    map<string>? properties = ();
};
