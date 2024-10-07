/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.healthcare.apim.consentmgt.mediation.cache;

import org.apache.commons.lang.StringUtils;
import org.wso2.healthcare.apim.consentmgt.mediation.FHIRConsentMgtException;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashGenerator {

    public static final String MD5_DIGEST_ALGORITHM = "MD5";

    public String getDigest(String text) throws FHIRConsentMgtException {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        byte[] digest = new byte[0];

        try {
            MessageDigest md = MessageDigest.getInstance(MD5_DIGEST_ALGORITHM);
            md.update((byte) 0);
            md.update((byte) 0);
            md.update((byte) 0);
            md.update((byte) 3);
            md.update(text.getBytes("UnicodeBigUnmarked"));
            digest = md.digest();

        } catch (NoSuchAlgorithmException e) {
            throw new FHIRConsentMgtException(
                    "Can not locate the algorithm " + "provided for the digest generation : " + MD5_DIGEST_ALGORITHM,
                    e);
        } catch (UnsupportedEncodingException e) {
            throw new FHIRConsentMgtException(
                    "Error in generating the digest " + "using the provided encoding : UnicodeBigUnmarked", e);
        }
        return new String(digest);
    }
}
