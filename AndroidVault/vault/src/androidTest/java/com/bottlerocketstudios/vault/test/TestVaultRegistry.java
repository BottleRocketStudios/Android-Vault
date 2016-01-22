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

import android.content.Context;
import android.test.AndroidTestCase;
import android.util.Log;

import com.bottlerocketstudios.vault.SharedPreferenceVault;
import com.bottlerocketstudios.vault.SharedPreferenceVaultFactory;
import com.bottlerocketstudios.vault.SharedPreferenceVaultRegistry;

import java.security.GeneralSecurityException;

/**
 * Ensure uniqueness in registry
 */
public class TestVaultRegistry extends AndroidTestCase {
    private static final String TAG = TestVaultRegistry.class.getSimpleName();

    private static final String KEY_FILE_NAME = "registryKeyFile";

    private static final String PREF_FILE_NAME_1 = "registryPrefFile1";
    private static final String KEY_ALIAS_1 = "keyAlias1";
    private static final int KEY_INDEX_1 = 1;
    private static final String PRESHARED_SECRET_1 = "a;sdlfkja;lkeiunwiuha;shdluifhe;l2ihjl9jl9dj9";

    private static final String PREF_FILE_NAME_2 = "registryPrefFile2";
    private static final String KEY_ALIAS_2 = "keyAlias2";
    private static final int KEY_INDEX_2 = 2;
    private static final String PRESHARED_SECRET_2 = "a;sdlfkja;asdfae22df23f545554656453458382328dfadsf;l2ihjl9jl9dj9";

    private static final String PREF_FILE_NAME_3 = "registryPrefFile3";
    private static final String KEY_ALIAS_3 = "keyAlias3";
    private static final int KEY_INDEX_3 = 3;


    public void testRegistryUniqueness() {
        SharedPreferenceVaultRegistry.getInstance().clear();

        addToVault(getContext(), PREF_FILE_NAME_1, KEY_FILE_NAME, KEY_ALIAS_1, KEY_INDEX_1, PRESHARED_SECRET_1);
        addToVault(getContext(), PREF_FILE_NAME_2, KEY_FILE_NAME, KEY_ALIAS_2, KEY_INDEX_2, PRESHARED_SECRET_2);

        assertNotNull("Shared preference vault was missing", SharedPreferenceVaultRegistry.getInstance().getVault(KEY_INDEX_1));

        boolean aliasRepetitionPrevented = false;
        try {
            addToVault(getContext(), PREF_FILE_NAME_3, KEY_FILE_NAME, KEY_ALIAS_2, KEY_INDEX_3, PRESHARED_SECRET_2);
        } catch (IllegalArgumentException e) {
            aliasRepetitionPrevented = true;
        }
        assertTrue("Registry allowed an alias collision", aliasRepetitionPrevented);

        boolean indexRepetitionPrevented = false;
        try {
            addToVault(getContext(), PREF_FILE_NAME_3, KEY_FILE_NAME, KEY_ALIAS_3, KEY_INDEX_2, PRESHARED_SECRET_2);
        } catch (IllegalArgumentException e) {
            indexRepetitionPrevented = true;
        }
        assertTrue("Registry allowed an index collision", indexRepetitionPrevented);

        boolean prefFileRepetitionPrevented = false;
        try {
            addToVault(getContext(), PREF_FILE_NAME_2, KEY_FILE_NAME, KEY_ALIAS_3, KEY_INDEX_3, PRESHARED_SECRET_2);
        } catch (IllegalArgumentException e) {
            prefFileRepetitionPrevented = true;
        }
        assertTrue("Registry allowed a pref file collision", prefFileRepetitionPrevented);
    }

    private void addToVault(Context context, String prefFileName, String keyFileName, String keyAlias, int keyIndex, String presharedSecret) {
        SharedPreferenceVault vault = null;
        try {
            vault = SharedPreferenceVaultFactory.getAppKeyedCompatAes256Vault(context, prefFileName, keyFileName, keyAlias, keyIndex, presharedSecret);
            SharedPreferenceVaultRegistry.getInstance().addVault(keyIndex, prefFileName, keyAlias, vault);
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "Caught java.security.GeneralSecurityException", e);
            assertTrue("Exception creating vault", false);
        }
    }

}
