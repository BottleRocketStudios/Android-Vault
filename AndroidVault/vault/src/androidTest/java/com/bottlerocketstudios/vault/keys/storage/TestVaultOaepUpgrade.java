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

package com.bottlerocketstudios.vault.keys.storage;

import android.os.Build;
import android.test.AndroidTestCase;
import android.util.Log;

import com.bottlerocketstudios.vault.EncryptionConstants;
import com.bottlerocketstudios.vault.keys.generator.Aes256RandomKeyFactory;
import com.bottlerocketstudios.vault.salt.PrngSaltGenerator;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.SecretKey;

/**
 * Created on 9/21/16.
 */
public class TestVaultOaepUpgrade extends AndroidTestCase {
    private static final String TAG = TestVaultUpgradePre18.class.getSimpleName();

    private static final String KEY_FILE_NAME = "OaepUpgradeKeyFile";
    private static final String KEY_ALIAS_1 = "OaepUpgradeKeyAlias";
    private static final int KEY_INDEX_1 = 1223422;
    private static final String PRESHARED_SECRET_1 = "a;sdlfkja;asdfa4548w1211xji22e;l2ihjl9jl9dj9";

    public void testUpgrade() {
        assertTrue("This test will not pass below API 23", Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
        try {
            SecretKey originalKey = Aes256RandomKeyFactory.createKey();
            KeyStorage keyStorageOld = getKeyStorage(CompatSharedPrefKeyStorageFactory.WRAPPER_TYPE_RSA_PKCS1, CompatSharedPrefKeyStorageFactory.WRAPPER_TYPE_RSA_PKCS1);
            assertEquals("Incorrect KeyStorageType", KeyStorageType.ANDROID_KEYSTORE, keyStorageOld.getKeyStorageType());
            keyStorageOld.clearKey(getContext());
            keyStorageOld.saveKey(getContext(), originalKey);

            SecretKey originalReadKey = keyStorageOld.loadKey(getContext());
            assertNotNull("Key was null after creation and read from old storage.", originalReadKey);
            assertTrue("Keys were not identical after creation and read from old storage", Arrays.equals(originalKey.getEncoded(), originalReadKey.getEncoded()));

            KeyStorage keyStorageNew = getKeyStorage(CompatSharedPrefKeyStorageFactory.WRAPPER_TYPE_RSA_PKCS1, CompatSharedPrefKeyStorageFactory.WRAPPER_TYPE_RSA_OAEP);
            assertEquals("Incorrect KeyStorageType", KeyStorageType.ANDROID_KEYSTORE, keyStorageNew.getKeyStorageType());
            SecretKey upgradedKey = keyStorageNew.loadKey(getContext());
            assertNotNull("Key was null after upgrade.", upgradedKey);
            assertTrue("Keys were not identical after upgrade", Arrays.equals(originalKey.getEncoded(), upgradedKey.getEncoded()));

            KeyStorage keyStorageRead = getKeyStorage(CompatSharedPrefKeyStorageFactory.WRAPPER_TYPE_RSA_OAEP, CompatSharedPrefKeyStorageFactory.WRAPPER_TYPE_RSA_OAEP);
            assertEquals("Incorrect KeyStorageType", KeyStorageType.ANDROID_KEYSTORE, keyStorageRead.getKeyStorageType());
            SecretKey upgradedReadKey = keyStorageRead.loadKey(getContext());
            assertNotNull("Key was null after upgrade and read from storage.", upgradedReadKey);
            assertTrue("Keys were not identical after upgrade and read from storage", Arrays.equals(originalKey.getEncoded(), upgradedReadKey.getEncoded()));

        } catch (GeneralSecurityException e) {
            Log.e(TAG, "Caught java.security.GeneralSecurityException", e);
            assertTrue("Exception when creating keystores", false);
        }

    }

    private KeyStorage getKeyStorage(int oldWrapperType, int newWrapperType) throws GeneralSecurityException {
        return CompatSharedPrefKeyStorageFactory.createKeyStorage(
                getContext(),
                Build.VERSION.SDK_INT,
                KEY_FILE_NAME,
                KEY_ALIAS_1,
                KEY_INDEX_1,
                EncryptionConstants.AES_CIPHER,
                PRESHARED_SECRET_1,
                new PrngSaltGenerator(),
                oldWrapperType,
                newWrapperType);
    }
}
