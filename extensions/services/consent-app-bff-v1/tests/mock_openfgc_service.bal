// Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the
// License at http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific
// language governing permissions and limitations under the License.

import ballerina/http;

// Mock Asgardeo token endpoint — prevents real network calls during module init
listener http:Listener mockAsgardeoListener = new (9096);

service / on mockAsgardeoListener {
    resource function post token(@http:Payload string payload) returns json {
        return {
            access_token: "mock-access-token",
            token_type: "Bearer",
            expires_in: 3600,
            scope: "openid"
        };
    }
}

listener http:Listener mockOpenfgcListener = new (9097);

service / on mockOpenfgcListener {

    // POST /consents — consent creation
    resource function post consents(@http:Payload json payload)
            returns http:Created {
        return {
            body: {
                id: "test-consent-id",
                status: "ACTIVE"
            }
        };
    }

    // GET /consents/attributes — attribute-based consent lookup (used by bind-consent-to-token)
    resource function get consents/attributes(string 'key = "", string value = "")
            returns json {
        return {
            consentIds: ["test-consent-id"],
            count: 1
        };
    }
}
