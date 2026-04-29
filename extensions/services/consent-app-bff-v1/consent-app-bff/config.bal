// Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).

// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at

// http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

# Asgardeo tenant domain (e.g. "anjucha")
configurable string tenantDomain = ?;

# Asgardeo OAuth2 token endpoint — defaults to the standard per-tenant URL
configurable string asgardeoTokenEndpoint = string `https://api.asgardeo.io/t/${tenantDomain}/oauth2/token`;

# Allowed CORS origin for the consent app UI (e.g. "https://consent.example.com")
configurable string corsAllowedOrigin = ?;

# OAuth2 client ID for the client credentials grant
configurable string clientId = ?;

# OAuth2 client secret for the client credentials grant
configurable string clientSecret = ?;

# OpenFGC organisation ID
configurable string orgId = ?;

# TPP client ID sent to OpenFGC when creating a consent
configurable string tppClientId = ?;

# Consent type string used when creating consents in OpenFGC
configurable string consentType = ?;

# Base URL of the OpenFGC consent service
configurable string openfgcBaseUrl = ?;

// When true, the BFF looks up an existing consent for the user+app combination and updates it on
// re-submit instead of creating a new one. When false (default), every auth flow creates a new consent.
configurable boolean singleConsentPerUser = false;

// When true (default), each purpose's elements are sent to the UI so the user can approve them
// individually. When false, elements are hidden and the whole purpose is approved or denied at once.
configurable boolean showConsentElements = true;

# Consent purposes presented to the user on the consent page.
# Each entry maps to one [[consentPurpose]] block in Config.toml.
configurable ConsentPurposeConfig[] consentPurpose = [
    {
        name: "All Health Data Access",
        description: "Access your health data related to USCDI.",
        mandatory: false,
        elements: [
            "MedicationDispense", "Coverage", "CareTeam", "RelatedPerson", "DiagnosticReport",
            "PractitionerRole", "Goal", "MedicationRequest", "Location", "CarePlan",
            "Provenance", "Patient", "Observation", "Condition", "ServiceRequest",
            "QuestionnaireResponse", "Medication", "Immunization", "Specimen", "Procedure",
            "Practitioner", "Organization", "Device", "AllergyIntolerance", "DocumentReference",
            "Encounter"
        ]
    }
];
