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

import android.content.SharedPreferences;
import android.test.AndroidTestCase;
import android.util.Log;

import com.bottlerocketstudios.vault.SharedPreferenceVault;
import com.bottlerocketstudios.vault.SharedPreferenceVaultFactory;
import com.bottlerocketstudios.vault.StandardSharedPreferenceVault;
import com.bottlerocketstudios.vault.keys.generator.Aes256RandomKeyFactory;

import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;

/*
 * Test to see if the listener is called correctly when it should be.
 */
public class TestSharedPrefListener extends AndroidTestCase {
    private static final String TAG = TestSharedPrefListener.class.getSimpleName();

    private static final String KEY_FILE_NAME = "listenerKeyTest";
    private static final String PREF_FILE_NAME = "listenerPrefTest";
    private static final String KEY_ALIAS_1 = "listenerKeyAlias";
    private static final int KEY_INDEX_1 = 1;
    private static final String PRESHARED_SECRET_1 = "a;sdl564546a6s6w2828d4fsdfbsijd;saj;9dj9";

    private static final String TEST_STRING_KEY = "testKey";
    private static final String TEST_STRING_VALUE = " This is a test. ";

    private SharedPreferenceVault mSharedPreferenceVault;

    // Initialization function. Should be called by all tests but only initialized once. On previous
    // invocations, the storage is cleared and that is it
    private boolean shouldInit = true;
    private void init() {
        Log.i(TAG, "Initializing");
        if(shouldInit) {
            mSharedPreferenceVault = null;
            try {
                Log.i(TAG, "Creating SharedPreference vault.");
                mSharedPreferenceVault = SharedPreferenceVaultFactory.getAppKeyedCompatAes256Vault(
                        getContext(),
                        PREF_FILE_NAME,
                        KEY_FILE_NAME,
                        KEY_ALIAS_1,
                        KEY_INDEX_1,
                        PRESHARED_SECRET_1);
            } catch (GeneralSecurityException e) {
                Log.e(TAG, "Caught java.security.GeneralSecurityException", e);
                assertTrue("Exception creating vault", false);
            }

            assertNotNull("Unable to create initial vault", mSharedPreferenceVault);
            assertTrue("Testing StandardSharedPreferenceVault",
                    mSharedPreferenceVault instanceof StandardSharedPreferenceVault);

            shouldInit = false;
        }
        cleanStorage();
    }

    private void cleanStorage() {
        mSharedPreferenceVault.rekeyStorage(Aes256RandomKeyFactory.createKey());
        assertNull("Rekey of storage did not clear existing value",
                mSharedPreferenceVault.getString(TEST_STRING_KEY, null));
    }

    public void testSuccessCommit() {
        init();
        SharedPreferences.Editor editor = mSharedPreferenceVault.setSharedPrefVaultWriteListener(
                new SharedPrefVaultWriteListenerExtension(new OnComplete() {
                    @Override
                    public void onComplete(boolean success) {
                        assertTrue("Error when committing to sharedPrefs", success);
                    }
                })
        ).edit();

        editor.putString(TEST_STRING_KEY, TEST_STRING_VALUE).commit();
    }

    public void testSuccessApply() {
        init();
        SharedPreferences.Editor editor = mSharedPreferenceVault.setSharedPrefVaultWriteListener(
                new SharedPrefVaultWriteListenerExtension(new OnComplete() {
                    @Override
                    public void onComplete(boolean success) {
                        assertTrue("Error when applying to sharedPrefs", success);
                    }
                })
        ).edit();

        editor.putString(TEST_STRING_KEY, TEST_STRING_VALUE).apply();
    }

    public void testNullSecretKeyFailureCommit() {
        init();
        mSharedPreferenceVault.clearStorage();
        SharedPreferences.Editor editor = mSharedPreferenceVault.setSharedPrefVaultWriteListener(
                new SharedPrefVaultWriteListenerExtension(new OnComplete() {
                    @Override
                    public void onComplete(boolean success) {
                        assertFalse("The key is still initialized", success);
                    }
                })
        ).edit();

        editor.putString(TEST_STRING_KEY, TEST_STRING_VALUE).commit();
    }

    public void testNullSecretKeyFailureApply() {
        init();
        mSharedPreferenceVault.clearStorage();
        SharedPreferences.Editor editor = mSharedPreferenceVault.setSharedPrefVaultWriteListener(
                new SharedPrefVaultWriteListenerExtension(new OnComplete() {
                    @Override
                    public void onComplete(boolean success) {
                        assertFalse("The key is still initialized", success);
                    }
                })
        ).edit();

        editor.putString(TEST_STRING_KEY, TEST_STRING_VALUE).apply();
    }

    private final SecretKey malformedSecretKey = new SecretKey() {
        private final String mKey = "ThisIsMySuperSecretKey";

        @Override
        public String getAlgorithm() {
            return "BestAlgorithEver";
        }

        @Override
        public String getFormat() {
            return mKey;
        }

        @Override
        public byte[] getEncoded() {
            return mKey.getBytes();
        }
    };
    public void testMalformedKeyFailureCommit() {
        init();
        mSharedPreferenceVault.rekeyStorage(malformedSecretKey);
        SharedPreferences.Editor editor = mSharedPreferenceVault.setSharedPrefVaultWriteListener(
                new SharedPrefVaultWriteListenerExtension(new OnComplete() {
                    @Override
                    public void onComplete(boolean success) {
                        assertFalse("Malformed key still works", success);
                    }
                })
        ).edit();

        editor.putString(TEST_STRING_KEY, TEST_STRING_VALUE).commit();
    }

    public void testMalformedKeyFailureApply() {
        init();
        mSharedPreferenceVault.rekeyStorage(malformedSecretKey);
        SharedPreferences.Editor editor = mSharedPreferenceVault.setSharedPrefVaultWriteListener(
                new SharedPrefVaultWriteListenerExtension(new OnComplete() {
                    @Override
                    public void onComplete(boolean success) {
                        assertFalse("Malformed key still works", success);
                    }
                })
        ).edit();

        editor.putString(TEST_STRING_KEY, TEST_STRING_VALUE).apply();
    }

    public interface OnComplete {
        void onComplete(boolean success);
    }

    private class SharedPrefVaultWriteListenerExtension implements SharedPreferenceVault.SharedPrefVaultWriteListener {
        private final OnComplete mListener;

        public SharedPrefVaultWriteListenerExtension(OnComplete listener) {
            mListener = listener;
        }

        @Override
        public void onSuccess() {
            mListener.onComplete(true);
        }

        @Override
        public void onError() {
            mListener.onComplete(false);
        }
    }
}
