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

package com.bottlerocketstudios.vault.keys.storage.hardware;

import android.content.Context;
import android.content.SharedPreferences;

import com.bottlerocketstudios.vault.keys.storage.AndroidKeystoreTestState;
import com.bottlerocketstudios.vault.keys.wrapper.AbstractAndroidKeystoreSecretKeyWrapper;

import java.security.GeneralSecurityException;

/**
 * Created on 9/21/16.
 */
public class LegacyAndroidKeystoreTester extends AndroidKeystoreTester {

    public LegacyAndroidKeystoreTester(Context context, String keystoreAlias, int currentSdkInt) {
        super(context, keystoreAlias, currentSdkInt);
    }

    @Override
    protected String getAndroidKeystoreTestStateSharedPreferenceKey(String keystoreAlias) {
        return PREF_COMPAT_FACTORY_ANDROID_KEYSTORE_TEST_STATE_ROOT + keystoreAlias;
    }

    @Override
    protected AbstractAndroidKeystoreSecretKeyWrapper createKeystoreSecretKeyWrapper(Context context, String testKeystoreAlias) throws GeneralSecurityException {
        throw new UnsupportedOperationException("Tests should not be performed. This class is provided to read legacy test status.");
    }

    public static boolean hasAlreadyPassedTest(Context context, String keystoreAlias, int currentSdkInt, SharedPreferences sharedPreferences) {
        LegacyAndroidKeystoreTester tester = new LegacyAndroidKeystoreTester(context, keystoreAlias, currentSdkInt);
        return AndroidKeystoreTestState.PASS.equals(tester.readAndroidKeystoreTestState(sharedPreferences));
    }
}
