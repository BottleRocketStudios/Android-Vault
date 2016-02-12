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

import android.annotation.SuppressLint;
import android.test.AndroidTestCase;
import android.util.Log;

import com.bottlerocketstudios.vault.SharedPreferenceVault;
import com.bottlerocketstudios.vault.SharedPreferenceVaultFactory;
import com.bottlerocketstudios.vault.keys.generator.Aes256RandomKeyFactory;
import com.bottlerocketstudios.vault.keys.storage.KeyStorageType;

import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;

/**
 * Test usage with a key that is only stored in memory.
 */
public class TestMemoryOnlyKey extends AndroidTestCase {

    private static final String TAG = TestMemoryOnlyKey.class.getSimpleName();

    private static final String PREF_FILE_NAME = "memoryOnlyPrefFile";

    private static final String TEST_STRING_KEY = "testKey";
    private static final String TEST_STRING_VALUE = " This is a test. ";

    @SuppressLint("CommitPrefEdits")
    public void testVaultRetention() {
        SharedPreferenceVault sharedPreferenceVault1 = null;
        try {
            sharedPreferenceVault1 = SharedPreferenceVaultFactory.getMemoryOnlyKeyAes256Vault(getContext(), PREF_FILE_NAME, false);
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "Caught java.security.GeneralSecurityException", e);
            assertTrue("Exception creating vault", false);
        }
        assertNotNull("Unable to create initial vault", sharedPreferenceVault1);

        assertFalse("Key was present before setting it", sharedPreferenceVault1.isKeyAvailable());
        assertNull("Reading data without setting key worked", sharedPreferenceVault1.getString(TEST_STRING_KEY, null));

        //Set a new random key
        SecretKey testKey1 = Aes256RandomKeyFactory.createKey();
        sharedPreferenceVault1.setKey(testKey1);
        assertTrue("Key was not present after setting it", sharedPreferenceVault1.isKeyAvailable());
        assertNull("Rekey of storage did not clear existing value", sharedPreferenceVault1.getString(TEST_STRING_KEY, null));
        assertEquals("Wrong type of storage", KeyStorageType.NOT_PERSISTENT, sharedPreferenceVault1.getKeyStorageType());

        //Store some data and verify it.
        sharedPreferenceVault1.edit().putString(TEST_STRING_KEY, TEST_STRING_VALUE).apply();
        assertEquals("Storage in initial vault did not work properly", TEST_STRING_VALUE, sharedPreferenceVault1.getString(TEST_STRING_KEY, null));

        //Create a secondary instance of the sharedPreferenceVault to ensure in-memory key is not shared implicitly.
        SharedPreferenceVault sharedPreferenceVault2 = null;
        try {
            sharedPreferenceVault2 = SharedPreferenceVaultFactory.getMemoryOnlyKeyAes256Vault(getContext(), PREF_FILE_NAME, false);
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "Caught java.security.GeneralSecurityException", e);
            assertTrue("Exception creating vault", false);
        }
        assertNotNull("Unable to create second instance of vault", sharedPreferenceVault2);
        assertNull("Retrieval in second vault worked without key.", sharedPreferenceVault2.getString(TEST_STRING_KEY, null));

        //Apply key and test again
        sharedPreferenceVault2.setKey(testKey1);
        assertEquals("Retrieval in second instance of vault did not work properly", TEST_STRING_VALUE, sharedPreferenceVault1.getString(TEST_STRING_KEY, null));

        //Clear key and verify failure
        sharedPreferenceVault2.setKey(null);
        assertFalse("Key was not cleared", sharedPreferenceVault2.isKeyAvailable());
        assertNull("Retrieval in second vault worked after clearing key.", sharedPreferenceVault2.getString(TEST_STRING_KEY, null));

        //Test incorrect key
        sharedPreferenceVault2.setKey(Aes256RandomKeyFactory.createKey());
        assertTrue("Rekey did not work", sharedPreferenceVault2.isKeyAvailable());
        assertNull("Retrieval in second vault worked after clearing key.", sharedPreferenceVault2.getString(TEST_STRING_KEY, null));

        //Test data clearing in initial vault
        sharedPreferenceVault1.clearStorage();
        assertFalse("Key was not removed after clearing storage.", sharedPreferenceVault1.isKeyAvailable());
        sharedPreferenceVault1.rekeyStorage(testKey1);
        assertNull("Clear storage failed to delete data", sharedPreferenceVault1.getString(TEST_STRING_KEY, null));
    }
}
