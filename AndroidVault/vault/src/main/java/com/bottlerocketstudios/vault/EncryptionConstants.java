/*
 * Copyright (c) 2016. Bottle Rocket LLC
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bottlerocketstudios.vault;

/**
 * Constant values used by the various encryption methods.
 */
public class EncryptionConstants {
    public static final String AES_CIPHER = "AES";
    public static final String BLOCK_MODE_CBC = "CBC";
    public static final String ENCRYPTION_PADDING_PKCS5 = "PKCS5Padding";
    public static final String ENCRYPTION_PADDING_PKCS7 = "PKCS7Padding";

    /**
      * While this specifies PKCS5Padding and a 256 bit key, a historical artifact in the Sun encryption
      * implementation interprets PKCS5 to be PKCS7 for block sizes over 8 bytes. In Android M this
      * appears to have been corrected so that PKCS7Padding will work when instantiating a Cipher object.
      */
    public static final String AES_CBC_PADDED_TRANSFORM = AES_CIPHER + "/" + BLOCK_MODE_CBC + "/" + ENCRYPTION_PADDING_PKCS5;
    public static final String AES_CBC_PADDED_TRANSFORM_ANDROID_M = AES_CIPHER + "/" + BLOCK_MODE_CBC + "/" + ENCRYPTION_PADDING_PKCS7;
    public static final int AES_256_KEY_LENGTH_BITS = 256;

    public static final String DIGEST_SHA256 = "SHA256";

    public static final String ANDROID_KEY_STORE = "AndroidKeyStore";
}
