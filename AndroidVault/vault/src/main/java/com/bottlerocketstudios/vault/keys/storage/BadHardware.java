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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by adam.newman on 2/9/16.
 */
public class BadHardware {

    private static final List<String> BAD_HARDWARE_MODELS = new ArrayList<>();
    static {
        BAD_HARDWARE_MODELS.add("SGH-T889"); //Galaxy Note 2 nukes hardware keystore on PIN unlock.
    }

    public static boolean isBadHardware() {
        return BAD_HARDWARE_MODELS.contains(Build.MODEL);
    }
}
