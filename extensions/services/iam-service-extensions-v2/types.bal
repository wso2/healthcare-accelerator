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

import ballerina/http;

// ─── Token action request ─────────────────────────────────────────────────────

type RequestParams record {
    string name;
    string[] value;
};

type RequestHeaders record {
    string name;
    string[] value;
};

type Request record {
    string grantType;
    string clientId;
    string[] scopes?;
    RequestHeaders[] additionalHeaders?;
    RequestParams[] additionalParams?;
};

type AccessTokenClaims record {
    string name?;
    string|int|boolean|string[] value?;
};

type AccessToken record {
    "JWT" tokenType;
    AccessTokenClaims[] claims;
    string[] scopes?;
};

type RefreshTokenClaims record {
    string name?;
    int value?;
};

type RefreshToken record {
    RefreshTokenClaims[] claims;
};

type Tenant record {
    string id;
    string name;
};

type Organization record {
    string id;
    string name;
    string orgHandle;
    int depth;
};

type UserStore record {
    string id;
    string name;
};

type User record {
    string id;
    Organization organization?;
};

// Open record — session and other dynamic fields live alongside defined ones
type Event record {
    Request request;
    Tenant tenant;
    Organization organization?;
    User user?;
    UserStore userStore?;
    AccessToken accessToken;
    RefreshToken refreshToken?;
};

type RequestBody record {
    string flowId?;
    string requestId?;
    "PRE_ISSUE_ACCESS_TOKEN" actionType;
    Event event;
};

// ─── Token action response ────────────────────────────────────────────────────

type CustomValue record {
    string name;
    string|int|boolean|string[] value;
};

type addOperationResponse record {
    "add" op;
    string path;
    string|CustomValue value;
};

type removeOperationResponse record {
    "remove" op;
    string path;
};

type replaceOperationResponse record {
    "replace" op;
    string path;
    string value;
};

type SuccessResponse record {
    "SUCCESS" actionStatus;
    (addOperationResponse|replaceOperationResponse|removeOperationResponse)[] operations?;
};

type ErrorResponse record {
    "ERROR" actionStatus;
    string errorMessage;
    string errorDescription;
};

type InlineResponse200Ok record {|
    *http:Ok;
    SuccessResponse body;
|};

type ErrorResponseBadRequest record {|
    *http:BadRequest;
    ErrorResponse body;
|};

type ErrorResponseInternalServerError record {|
    *http:InternalServerError;
    ErrorResponse body;
|};

// ─── EHR launch context ───────────────────────────────────────────────────────

type EhrLaunchContext record {|
    string patientId?;
    string encounterId?;
    string launchId;
    string aud;
    string expiry;
|};
