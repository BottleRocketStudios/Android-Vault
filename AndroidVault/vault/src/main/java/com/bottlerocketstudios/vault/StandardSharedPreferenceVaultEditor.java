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

import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Editor implementation for StandardSharedPreferenceVault
 */
public class StandardSharedPreferenceVaultEditor implements SharedPreferences.Editor {

    private final StandardSharedPreferenceVault mStandardSharedPreferenceVault;
    private StronglyTypedBundle mStronglyTypedBundle = new StronglyTypedBundle();
    private boolean mCleared;
    private Set<String> mRemovalSet = new HashSet<>();

    public StandardSharedPreferenceVaultEditor(StandardSharedPreferenceVault standardSharedPreferenceVault) {
        mStandardSharedPreferenceVault = standardSharedPreferenceVault;
    }

    @Override
    public SharedPreferences.Editor putString(String key, String value) {
        mStronglyTypedBundle.putValue(key, value);
        return this;
    }

    @Override
    public SharedPreferences.Editor putStringSet(String key, Set<String> value) {
        mStronglyTypedBundle.putValue(key, value);
        return this;
    }

    @Override
    public SharedPreferences.Editor putInt(String key, int value) {
        mStronglyTypedBundle.putValue(key, value);
        return this;
    }

    @Override
    public SharedPreferences.Editor putLong(String key, long value) {
        mStronglyTypedBundle.putValue(key, value);
        return this;
    }

    @Override
    public SharedPreferences.Editor putFloat(String key, float value) {
        mStronglyTypedBundle.putValue(key, value);
        return this;
    }

    @Override
    public SharedPreferences.Editor putBoolean(String key, boolean value) {
        mStronglyTypedBundle.putValue(key, value);
        return this;
    }

    @Override
    public SharedPreferences.Editor remove(String key) {
        mStronglyTypedBundle.remove(key);
        mRemovalSet.add(key);
        return this;
    }

    @Override
    public SharedPreferences.Editor clear() {
        mCleared = true;
        return this;
    }

    @Override
    public boolean commit() {
        return mStandardSharedPreferenceVault.writeValues(true, mCleared, mRemovalSet, mStronglyTypedBundle);
    }

    @Override
    public void apply() {
        mStandardSharedPreferenceVault.writeValues(false, mCleared, mRemovalSet, mStronglyTypedBundle);
    }
}
