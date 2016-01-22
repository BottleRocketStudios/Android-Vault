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

package com.bottlerocketstudios.vault.test;

import android.test.AndroidTestCase;

import com.bottlerocketstudios.vault.EncryptionConstants;
import com.bottlerocketstudios.vault.keys.generator.Aes256KeyFromPasswordFactory;
import com.bottlerocketstudios.vault.keys.generator.Aes256RandomKeyFactory;

import javax.crypto.SecretKey;

/**
 * Test secret key creation.
 */
public class TestKeyGeneration extends AndroidTestCase {

    public void testRandomKey() {
        SecretKey secretKey = Aes256RandomKeyFactory.createKey();
        assertNotNull("Secret key was not created", secretKey);
        assertEquals("Secret key was incorrect length", secretKey.getEncoded().length, EncryptionConstants.AES_256_KEY_LENGTH_BITS / 8);
    }

    public void testPasswordKey() {
        SecretKey secretKey = Aes256KeyFromPasswordFactory.createKey("testPassword", 10000);
        assertNotNull("Secret key was not created", secretKey);
        assertEquals("Secret key was incorrect length", secretKey.getEncoded().length, EncryptionConstants.AES_256_KEY_LENGTH_BITS / 8);
    }

}
