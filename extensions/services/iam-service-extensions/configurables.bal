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

configurable string hostname = "localhost";
configurable int port = 9093;

// EHR context resolution (optional — leave blank to skip)
configurable string ehrContextResolveUrl = "";

// IS base URL for SCIM, consent management and introspect endpoints (e.g. https://host:9443)
configurable string isBaseUrl = "";
// Consent management API path on IS (IS 7.3.0+). For a tenant use /t/<tenant-domain>/api/identity/consent-mgt/v2.0
configurable string consentApiPath = "/api/identity/consent-mgt/v2.0";
// Management application credentials. The same application is used for the SCIM user lookup and the
// consent management API, so it must be authorized for internal_user_mgt_view and internal_consent_mgt_consent_view.
// (optional for SCIM — used to resolve patient ID from logged-in user)
configurable string scimApiPath = "/scim2/Users";
configurable string scimClientId = "";
configurable string scimClientSecret = "";
// Leave empty to default to {isBaseUrl}/oauth2/token
configurable string scimTokenEndpoint = "";
configurable string scimPatientGroupName = "patient";
configurable string fhirUserAttributeName = "fhirUser";
configurable string patientAttributeName = "patient";

// Key store for the HTTPS listener (leave blank to run as HTTP)
configurable string keystorePath = "";
configurable string keystorePassword = "";

// Trust store for HTTPS connections to the IDP/SCIM/EHR endpoints
configurable string trustStorePath = "";
configurable string trustStorePassword = "";

// Scopes that bypass consent checks (always included in token)
configurable string[] alwaysAllowedScopes = ["openid"];

// Service ID that consent-app-bff records on every consent. Used to find a user's consent when IS does not
// pass sessionDataKeyConsent to the token flow.
configurable string consentServiceId = "smart-on-fhir";
