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

import android.util.SparseArray;

import java.util.HashSet;
import java.util.Set;

/**
 * Ensure app-wide uniqueness of vault indices and key alias and reducing memory churn on object instantiation
 * and intermediate steps. Also ensures that vaults are single instance to avoid issues when the repository
 * has a key change or clearing event on a separate thread.
 *
 * This should be populated once in the Application Object's onCreate().
 *
 * Indices do not need to be consecutive, but they must be unique across the application and consistent
 * across upgrades.
 */
public class SharedPreferenceVaultRegistry {

    SparseArray<SharedPreferenceVault> mSharedPreferenceVaultArray;
    Set<String> mKeyAliasSet;
    Set<String> mPrefFileSet;

    private SharedPreferenceVaultRegistry() {
        mSharedPreferenceVaultArray = new SparseArray<>();
        mKeyAliasSet = new HashSet<>();
        mPrefFileSet = new HashSet<>();
    }

    /**
     * SingletonHolder is loaded on the first execution of Singleton.getInstance()
     * or the first access to SingletonHolder.INSTANCE, not before.
     */
    private static class SingletonHolder {
        public static final SharedPreferenceVaultRegistry instance = new SharedPreferenceVaultRegistry();
    }

    /**
     * Get the instance or create it. (inherently thread safe Bill Pugh pattern)
     */
    public static SharedPreferenceVaultRegistry getInstance() {
        return SingletonHolder.instance;
    }

    public void addVault(int index, String prefFileName, String keyAlias, SharedPreferenceVault vault) {
        if (mPrefFileSet.contains(prefFileName)) {
            throw new IllegalArgumentException("Only one vault per application can use the same preference file.");
        }
        if (mKeyAliasSet.contains(keyAlias)) {
            throw new IllegalArgumentException("Only one vault per application can use the same KeyAlias.");
        }
        if (mSharedPreferenceVaultArray.get(index) != null) {
            throw new IllegalArgumentException("Only one vault per application can use the same index.");
        }
        replaceVault(index, prefFileName, keyAlias, vault);
    }

    public void replaceVault(int index, String prefFileName, String keyAlias, SharedPreferenceVault vault) {
        mPrefFileSet.add(prefFileName);
        mKeyAliasSet.add(keyAlias);
        mSharedPreferenceVaultArray.put(index, vault);
    }

    public SharedPreferenceVault getVault(int index) {
        return mSharedPreferenceVaultArray.get(index);
    }

    public void clear() {
        mPrefFileSet.clear();
        mKeyAliasSet.clear();
        mSharedPreferenceVaultArray.clear();
    }

}
