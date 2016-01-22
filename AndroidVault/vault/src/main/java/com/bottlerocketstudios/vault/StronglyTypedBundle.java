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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Bundle that retains type information to be recovered on commit.
 */
public class StronglyTypedBundle {

    public Map<String, Class> mClassMap = new HashMap<>();
    public Map<String, Object> mValueMap = new HashMap<>();

    public Class getTypeForValue(String key) {
        return mClassMap.get(key);
    }

    public <T> T getValue(Class<T> type, String key) {
        return type.cast(mValueMap.get(key));
    }

    public void putValue(String key, Object value) {
        mValueMap.put(key, value);
        mClassMap.put(key, value.getClass());
    }

    public Set<String> keySet() {
        return mValueMap.keySet();
    }

    public void remove(String key) {
        mValueMap.remove(key);
        mClassMap.remove(key);
    }
}
