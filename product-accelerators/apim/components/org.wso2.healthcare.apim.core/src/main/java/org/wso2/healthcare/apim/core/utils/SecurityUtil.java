/*
 * Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.healthcare.apim.core.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.healthcare.apim.core.OpenHealthcareException;

/**
 * Utility class containing security related utility functions
 */
public class SecurityUtil {

    private static final Log LOG = LogFactory.getLog(SecurityUtil.class);

    /**
     * Wrapper utility function to encrypt and base64 encode
     *
     * @param plaintext
     * @return
     * @throws OpenHealthcareException
     */
    public static String encryptAndBase64Encode(byte[] plaintext) throws OpenHealthcareException {
        try {
            return CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(plaintext);
        } catch (CryptoException e) {
            throw new OpenHealthcareException("Error occurred while encrypting sensitive data", e);
        }
    }

    /**
     * Wrapper utility function to encrypt and base64 encode char array
     *
     * @param plaintext
     * @return
     * @throws OpenHealthcareException
     */
    public static String encryptAndBase64Encode(char[] plaintext) throws OpenHealthcareException {
        try {
            byte[] plainByteArray = null;
            if (plaintext != null) {
                plainByteArray = new String(plaintext).getBytes();
            }
            return CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(plainByteArray);
        } catch (CryptoException e) {
            throw new OpenHealthcareException("Error occurred while encrypting sensitive data", e);
        }
    }

    /**
     * Wrapper utility function to base64 decode and decrypt
     *
     * @param cipherText
     * @return
     * @throws OpenHealthcareException
     */
    public static byte[] base64DecodeAndDecrypt(String cipherText) throws OpenHealthcareException {
        try {
            return CryptoUtil.getDefaultCryptoUtil().base64DecodeAndDecrypt(cipherText);
        } catch (CryptoException e) {
            throw new OpenHealthcareException("Error occurred while decrypting cipher text", e);
        }
    }

    /**
     * Wrapper utility function to base64 decode and decrypt to char array
     *
     * @param cipherText
     * @return
     * @throws OpenHealthcareException
     */
    public static char[] base64DecodeAndDecryptToChars(String cipherText) throws OpenHealthcareException {
        try {
            return new String(CryptoUtil.getDefaultCryptoUtil().base64DecodeAndDecrypt(cipherText)).toCharArray();
        } catch (CryptoException e) {
            throw new OpenHealthcareException("Error occurred while decrypting cipher text", e);
        }
    }

}
