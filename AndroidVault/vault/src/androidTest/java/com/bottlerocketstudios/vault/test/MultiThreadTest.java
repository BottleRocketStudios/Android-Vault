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

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Test with a lot of concurrent threads.
 */
public class MultiThreadTest extends AndroidTestCase {
    private static final String TAG = MultiThreadTest.class.getSimpleName();

    private static final String KEY_FILE_NAME = "multiThreadKeyFile";
    private static final String PREF_FILE_NAME = "multiThreadPrefFile";
    private static final String KEY_ALIAS_1 = "multiThreadKeyAlias";
    private static final int KEY_INDEX_1 = 1;
    private static final String PRESHARED_SECRET_1 = "a;sdl564546a6s6w2828d4fsdfbsijd;saj;9dj9";

    private static final String TEST_KEY = "testKey";
    private static final String TEST_VALUE = "testValue";
    private static final int NUMBER_OF_SIMULTANEOUS_THREADS = 60;
    private static final long FUTURE_GET_TIMEOUT = 100;
    private static final int NUMBER_OF_ITERATIONS = 10;

    @SuppressLint("CommitPrefEdits")
    public void testWithManyThreads() {
        ExecutorService executorService = ThreadPoolExecutorWithExceptions.newCachedThreadPool();
        SharedPreferenceVault sharedPreferenceVault = null;
        try {
            sharedPreferenceVault = SharedPreferenceVaultFactory.getAppKeyedCompatAes256Vault(getContext(), PREF_FILE_NAME, KEY_FILE_NAME, KEY_ALIAS_1, KEY_INDEX_1, PRESHARED_SECRET_1);
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "Caught java.security.GeneralSecurityException", e);
            assertTrue("Exception creating vault", false);
        }

        for (int testIteration = 0; testIteration < NUMBER_OF_ITERATIONS; testIteration++) {
            sharedPreferenceVault.edit().putString(TEST_KEY, TEST_VALUE).apply();

            List<Future<String>> resultFutureList = new ArrayList<>(NUMBER_OF_SIMULTANEOUS_THREADS);
            for (int i = 0; i < NUMBER_OF_SIMULTANEOUS_THREADS; i++) {
                resultFutureList.add(executorService.submit(new GetVaultValueCallable(sharedPreferenceVault)));
            }

            int equalityCounter = 0;
            while (resultFutureList.size() > 0) {
                for (Iterator<Future<String>> resultFutureIterator = resultFutureList.iterator(); resultFutureIterator.hasNext(); ) {
                    Future<String> resultFuture = resultFutureIterator.next();
                    if (resultFuture.isDone()) {
                        try {
                            equalityCounter += TEST_VALUE.equals(resultFuture.get(FUTURE_GET_TIMEOUT, TimeUnit.MILLISECONDS)) ? 1 : 0;
                        } catch (InterruptedException e) {
                            Log.e(TAG, "Caught java.lang.InterruptedException", e);
                        } catch (ExecutionException e) {
                            Log.e(TAG, "Caught java.util.concurrent.ExecutionException", e);
                        } catch (TimeoutException e) {
                            Log.e(TAG, "Caught java.util.concurrent.TimeoutException", e);
                        }
                        resultFutureIterator.remove();
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Caught java.lang.InterruptedException", e);
                }
            }

            sharedPreferenceVault.edit().clear().commit();

            assertEquals("All tests didn't result in correct value on iteration " + testIteration, NUMBER_OF_SIMULTANEOUS_THREADS, equalityCounter);
        }
    }

    private class GetVaultValueCallable implements Callable<String> {
        private SharedPreferenceVault mSharedPreferenceVault;

        public GetVaultValueCallable(SharedPreferenceVault sharedPreferenceVault) {
            mSharedPreferenceVault = sharedPreferenceVault;
        }

        @Override
        public String call() throws Exception {
            return mSharedPreferenceVault.getString(TEST_KEY, null);
        }
    }

}

