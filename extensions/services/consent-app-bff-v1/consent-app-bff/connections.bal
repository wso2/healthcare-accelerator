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

import ballerina/http;
import ballerina/oauth2;

// Shared grant config — readonly so it is safe to access from isolated functions
final oauth2:ClientCredentialsGrantConfig & readonly asgardeoGrantConfig = {
    tokenUrl: asgardeoTokenEndpoint,
    clientId: clientId,
    clientSecret: clientSecret
};

// Plain HTTP client — auth header is set manually so we can intercept 401s
final http:Client asgardeoClient = check new (
    string `https://api.asgardeo.io/t/${tenantDomain}`
);

// Returns a fresh bearer token from Asgardeo.
// Creating the ClientOAuth2Provider here (rather than at module level) avoids
// an eager token fetch during module init — which would fail in tests before
// the mock listener is up.  Since callAsgardeo() is mocked in tests this
// function is never called during `bal test`.
isolated function getAsgardeoToken() returns string|error {
    oauth2:ClientOAuth2Provider tokenProvider = new (asgardeoGrantConfig);
    return check tokenProvider.generateToken();
}

// Plain HTTP client for the OpenFGC consent service — no auth required.
final http:Client openfgcClient = check new (openfgcBaseUrl);
