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

# Configuration record for a single consent purpose.
# Used in Config.toml as [[consentPurpose]] array-of-tables entries.
type ConsentPurposeConfig record {|
    # Unique name; also used as the purpose identifier in OpenFGC
    string name;
    # Optional human-readable description shown in the consent UI
    string description?;
    # Whether consent to this purpose is mandatory to proceed (default: false)
    boolean mandatory = false;
    # OpenFGC consent elements belonging to this purpose
    string[] elements;
|};

# Represents a consent purpose that the user is asked to approve.
public type Purpose record {|
    # Human-readable name of the purpose (also used as the unique identifier)
    string purposeName;
    # Whether consent to this purpose is mandatory to proceed
    boolean mandatory;
    # Optional description shown below the purpose name in the consent UI
    string purposeDescription?;
    # Element names belonging to this purpose, for element-level selection in the UI
    string[] elements;
|};

# Request payload for submitting a consent decision.
public type ConsentSubmission record {|
    # The session data key identifying the consent session
    string sessionDataKeyConsent;
    # The service provider ID requesting consent
    string spId;
    # The user's consent decision per purpose
    ConsentedPurpose[] consentedPurposes;
    # Whether the user approved (true) or denied (false) consent overall
    boolean approved;
    # Existing consent ID echoed from ConsentPageData.existingConsentId (singleConsentPerUser mode only)
    string existingConsentId?;
    # BFF-signed HS256 token from get-consent-data — server extracts trusted loggedInUser and application from this
    string consentToken;
|};

# Represents a purpose the user has approved, with the specific elements they consented to.
public type ConsentedPurpose record {|
    # Name of the approved purpose
    string purposeName;
    # Names of the elements within this purpose that the user approved
    string[] consentedElements;
|};

# Response shape from the Asgardeo OauthConsentKey API.
# Open record — only the fields we use are declared; extra fields are ignored.
type AsgardeoConsentKeyResponse record {
    # Name of the service provider (application)
    string application;
    # Space-separated OAuth scopes requested
    string scope;
    # The logged-in user who initiated the consent flow
    string loggedInUser;
};

# Response returned by the get-consent-data endpoint on page load.
public type ConsentPageData record {|
    # Name of the application requesting consent
    string appName;
    # OAuth scope string returned by the identity server
    string scope;
    # The logged-in user who initiated the consent flow
    string loggedInUser;
    # List of purposes requiring the user's consent
    Purpose[] purposes;
    # ID of the user's existing consent for this app, if one was found (singleConsentPerUser mode only)
    string existingConsentId?;
    # Names of purposes the user previously approved (singleConsentPerUser mode only)
    string[] previouslyConsentedPurposeNames?;
    # Per-purpose map of element names the user previously approved (singleConsentPerUser mode only)
    # Key: purposeName, Value: list of previously approved element names
    map<string[]> previouslyConsentedElements?;
    # Short-lived BFF-signed HS256 token binding loggedInUser and application to this session.
    # Must be sent back in submit-consent so the server can verify identity without re-calling Asgardeo.
    string consentToken;
|};

# Response returned after a consent submission.
public type ConsentSubmissionResponse record {|
    # Outcome of the submission: "success" or "error"
    string status;
    # Human-readable message
    string message;
|};

// --- Minimal OpenFGC types for consent creation ---

type OpenFGCConsentElementApproval record {
    string name;
    boolean isUserApproved;
};

type OpenFGCConsentPurposeItem record {
    string name;
    OpenFGCConsentElementApproval[] elements;
};

type OpenFGCAuthorizationResources record {
    string spId;
    string application;
};

type OpenFGCAuthorization record {
    string userId;
    string 'type;
    string status;
    OpenFGCAuthorizationResources resources;
};

type OpenFGCConsentCreatePayload record {
    string 'type;
    OpenFGCConsentPurposeItem[] purposes;
    OpenFGCAuthorization[] authorizations;
    # Top-level key-value attributes on the consent (used for querying)
    map<string> attributes?;
};

type OpenFGCConsentCreatedResponse record {
    string id?;
    string status?;
};

# Response from GET /consents/attributes in OpenFGC.
type OpenFGCConsentAttributeSearchResponse record {|
    # Consent IDs that match the attribute filter
    string[] consentIds;
    # Total number of matches
    int count;
|};

# Nested session object in the Asgardeo pre-issue-access-token action request.
# Open record — Asgardeo may include additional session fields.
type PreIssueTokenSession record {
    # The consent session key passed to the consent page
    string sessionDataKeyConsent?;
};

# Nested event object in the Asgardeo pre-issue-access-token action request.
# Open record — Asgardeo sends many event fields (request, tenant, user, accessToken, etc.).
type PreIssueTokenEvent record {
    # Session data from the authorization flow
    PreIssueTokenSession session?;
};

# Request payload from Asgardeo for the pre-issue-access-token action.
# Open record — Asgardeo sends additional top-level fields (flowId, requestId, allowedOperations, etc.).
type PreIssueTokenActionRequest record {
    # Always "PRE_ISSUE_ACCESS_TOKEN"
    string actionType?;
    # Flow and request context
    PreIssueTokenEvent event?;
};

# A single JSON Patch operation to include in the action response.
type TokenOperation record {|
    # Operation type: "add", "replace", or "remove"
    string op;
    # JSON Patch path (e.g. "/accessToken/claims/-")
    string path;
    # Value to set (for add/replace operations)
    json value?;
|};

# Success response returned to Asgardeo after processing the action.
type PreIssueTokenActionResponse record {|
    # "SUCCESS" or "FAILURE"
    string actionStatus;
    # List of patch operations to apply to the token
    TokenOperation[] operations?;
|};

// --- OpenFGC GET /consents search response types ---
// Open records: extra fields from OpenFGC (value, isMandatory, etc.) are silently ignored.

type OpenFGCConsentSearchElement record {
    string name;
    boolean isUserApproved;
};

type OpenFGCConsentSearchPurpose record {
    string name;
    OpenFGCConsentSearchElement[] elements;
};

type OpenFGCConsentSearchRecord record {
    string id;
    OpenFGCConsentSearchPurpose[] purposes?;
};

type OpenFGCConsentSearchResponse record {
    OpenFGCConsentSearchRecord[] data;
};

