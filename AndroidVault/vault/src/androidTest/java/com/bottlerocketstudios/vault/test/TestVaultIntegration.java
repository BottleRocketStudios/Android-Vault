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
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Integration test of normal operation.
 */
public class TestVaultIntegration extends AndroidTestCase {
    private static final String TAG = TestVaultIntegration.class.getSimpleName();

    private static final String KEY_FILE_NAME = "integrationKeyFile";

    private static final String PREF_FILE_NAME = "integrationPrefFile";
    private static final String KEY_ALIAS_1 = "integrationKeyAlias";
    private static final int KEY_INDEX_1 = 1;
    private static final String PRESHARED_SECRET_1 = "a;sdlfkja;5585585;shdluifhe;l2ihjl9jl9dj9";

    private static final String TEST_STRING_KEY = "testKey";
    private static final String TEST_STRING_VALUE = " This is a test. ";
    private static final String TEST_BOOLEAN_KEY = "testBooleanKey";
    private static final boolean TEST_BOOLEAN_VALUE = true;
    private static final String TEST_INT_KEY = "testIntegerKey";
    private static final int TEST_INT_VALUE = -230;
    private static final String TEST_LONG_KEY = "testLongKey";
    private static final long TEST_LONG_VALUE = Long.MAX_VALUE;
    private static final String TEST_FLOAT_KEY = "testFloatKey";
    private static final float TEST_FLOAT_VALUE = -2.3f;
    private static final String TEST_STRING_SET_KEY = "testStringSetKey";
    private static final Set<String> TEST_STRING_SET_VALUE;
    private static final int LARGE_STRING_SIZE = 8192;
    private static final String TEST_LARGE_STRING_KEY = "testLongStringKey";

    static {
        Set<String> stringSet = new HashSet<>();
        stringSet.add("Test String One");
        stringSet.add("Test String Two");
        stringSet.add("Test String Three");
        stringSet.add("Test String Four");
        TEST_STRING_SET_VALUE = stringSet;
    }

    @SuppressLint("CommitPrefEdits")
    public void testVaultRetention() {
        SharedPreferenceVault sharedPreferenceVault1 = null;
        try {
            sharedPreferenceVault1 = SharedPreferenceVaultFactory.getAppKeyedCompatAes256Vault(getContext(), PREF_FILE_NAME, KEY_FILE_NAME, KEY_ALIAS_1, KEY_INDEX_1, PRESHARED_SECRET_1);
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "Caught java.security.GeneralSecurityException", e);
            assertTrue("Exception creating vault", false);
        }
        assertNotNull("Unable to create initial vault", sharedPreferenceVault1);
        assertEquals("This test must be ran on a V18+ device with working Android Keystore support", KeyStorageType.ANDROID_KEYSTORE, sharedPreferenceVault1.getKeyStorageType());

        //Ensure no leftover data is restored
        sharedPreferenceVault1.rekeyStorage(Aes256RandomKeyFactory.createKey());
        assertNull("Rekey of storage did not clear existing value", sharedPreferenceVault1.getString(TEST_STRING_KEY, null));

        //Store some data and verify it.
        sharedPreferenceVault1.edit().putString(TEST_STRING_KEY, TEST_STRING_VALUE).apply();
        assertEquals("Storage in initial vault did not work properly", TEST_STRING_VALUE, sharedPreferenceVault1.getString(TEST_STRING_KEY, null));

        sharedPreferenceVault1.edit().putBoolean(TEST_BOOLEAN_KEY, TEST_BOOLEAN_VALUE).apply();
        assertEquals("Storage in initial vault did not work properly", TEST_BOOLEAN_VALUE, sharedPreferenceVault1.getBoolean(TEST_BOOLEAN_KEY, !TEST_BOOLEAN_VALUE));

        sharedPreferenceVault1.edit().putInt(TEST_INT_KEY, TEST_INT_VALUE).apply();
        assertEquals("Storage in initial vault did not work properly", TEST_INT_VALUE, sharedPreferenceVault1.getInt(TEST_INT_KEY, 0));

        sharedPreferenceVault1.edit().putLong(TEST_LONG_KEY, TEST_LONG_VALUE).apply();
        assertEquals("Storage in initial vault did not work properly", TEST_LONG_VALUE, sharedPreferenceVault1.getLong(TEST_LONG_KEY, 0));

        sharedPreferenceVault1.edit().putFloat(TEST_FLOAT_KEY, TEST_FLOAT_VALUE).apply();
        assertEquals("Storage in initial vault did not work properly", TEST_FLOAT_VALUE, sharedPreferenceVault1.getFloat(TEST_FLOAT_KEY, 0.0f));

        sharedPreferenceVault1.edit().putStringSet(TEST_STRING_SET_KEY, TEST_STRING_SET_VALUE).apply();
        assertEquals("Storage in initial vault did not work properly", TEST_STRING_SET_VALUE, sharedPreferenceVault1.getStringSet(TEST_STRING_SET_KEY, null));

        //Test getAll type checking operation.
        Map<String, Object> fullSet = (Map<String, Object>) sharedPreferenceVault1.getAll();
        assertTrue("String was not correct type", fullSet.get(TEST_STRING_KEY) instanceof String);
        assertTrue("Boolean was not correct type", fullSet.get(TEST_BOOLEAN_KEY) instanceof Boolean);
        assertTrue("Integer was not correct type", fullSet.get(TEST_INT_KEY) instanceof Integer);
        assertTrue("Long was not correct type", fullSet.get(TEST_LONG_KEY) instanceof Long);
        assertTrue("Float was not correct type", fullSet.get(TEST_FLOAT_KEY) instanceof Float);
        assertEquals("Set was not correct type", TEST_STRING_SET_VALUE, fullSet.get(TEST_STRING_SET_KEY));

        //Clear data except for the test string key.
        assertTrue("Contains test did not work", sharedPreferenceVault1.contains(TEST_BOOLEAN_KEY));
        sharedPreferenceVault1.edit().clear().putString(TEST_STRING_KEY, TEST_STRING_VALUE).commit();
        assertFalse("Clear operation did not work", sharedPreferenceVault1.contains(TEST_BOOLEAN_KEY));

        //Create a secondary instance of the sharedPreferenceVault to ensure separate data reading works and instantiation doesn't clobber old key/data.
        SharedPreferenceVault sharedPreferenceVault2 = null;
        try {
            sharedPreferenceVault2 = SharedPreferenceVaultFactory.getAppKeyedCompatAes256Vault(getContext(), PREF_FILE_NAME, KEY_FILE_NAME, KEY_ALIAS_1, KEY_INDEX_1, PRESHARED_SECRET_1);
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "Caught java.security.GeneralSecurityException", e);
            assertTrue("Exception creating vault", false);
        }
        assertNotNull("Unable to create second instance of vault", sharedPreferenceVault2);
        assertEquals("Retrieval in second vault did not work properly", TEST_STRING_VALUE, sharedPreferenceVault2.getString(TEST_STRING_KEY, null));

        //Test very large string
        final String veryLargeString = createRandomString(LARGE_STRING_SIZE);
        sharedPreferenceVault1.edit().putString(TEST_LARGE_STRING_KEY, veryLargeString).commit();
        assertEquals("Very long string mismatch", veryLargeString, sharedPreferenceVault1.getString(TEST_LARGE_STRING_KEY, null));

        //Test data clearing
        sharedPreferenceVault1.clearStorage();
        assertFalse("Key was not removed", sharedPreferenceVault1.isKeyAvailable());
        assertNull("Clear storage failed to delete data", sharedPreferenceVault1.getString(TEST_STRING_KEY, null));
    }

    private String createRandomString(int size) {
        StringBuilder stringBuilder = new StringBuilder();
        final String validCharacters = "0123456789abcdefghijlmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWYXZ\n\t ";
        final int validCharacterLength = validCharacters.length();
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            stringBuilder.append(validCharacters.charAt(random.nextInt(validCharacterLength)));
        }
        return stringBuilder.toString();
    }

}
