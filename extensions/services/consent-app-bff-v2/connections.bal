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

import ballerina/http;
import ballerina/oauth2;

// Plain HTTP clients — auth headers are set manually so 401s can be intercepted.
final http:Client idpClient = check new (idpBaseUrl);
final http:Client openfgcClient = check new (openfgcBaseUrl);

// Returns a fresh bearer token from the identity provider using client credentials.
// A new ClientOAuth2Provider is created per call to avoid an eager token fetch at
// module init, which would fail in tests before mock listeners are up.
isolated function getIdpToken() returns string|error {
    string tokenUrl = idpTokenEndpoint == "" ? string `${idpBaseUrl}/oauth2/token` : idpTokenEndpoint;
    oauth2:ClientCredentialsGrantConfig grantConfig = {
        tokenUrl: tokenUrl,
        clientId: clientId,
        clientSecret: clientSecret,
        scopes: ["internal_user_mgt_view"]
    };
    oauth2:ClientOAuth2Provider provider = new (grantConfig);
    return check provider.generateToken();
}
