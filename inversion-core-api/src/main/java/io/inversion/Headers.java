/*
 * Copyright (c) 2015-2022 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.inversion;

import io.inversion.json.JSList;
import io.inversion.json.JSMap;
import io.inversion.utils.ListMap;

import java.util.ArrayList;
import java.util.List;

public interface Headers{

    JSMap getHeaders();
    default JSMap lazyHeaders(){
        return getHeaders();
    };

    default String getHeader(String name) {
        if(getHeaders() == null)
            return null;
        JSList list = lazyHeaders().getList(name);
        if (list != null && list.size() > 0)
            return list.getString(0);

        return null;
    }

    default List<String> getAllHeaders(String name) {
        if(getHeaders() == null)
            return null;

        JSList list = lazyHeaders().getList(name);
        if(list == null)
            return new ArrayList();
        return new ArrayList(list);
    }

    default void setHeader(String name, String value){
        removeHeader(name);
        addHeader(name, value);
    }

    default void addHeader(String name, String value) {
        JSList list    = (JSList)lazyHeaders().get(name);
        if (list == null) {
            list = new JSList();
            lazyHeaders().put(name, list);
        }
        if (!list.contains(value))
            list.add(value);
    }

    default void addHeaders(String... headerNameValuePairs) {
        for (int i = 0; headerNameValuePairs != null && i < headerNameValuePairs.length; i += 2) {
            String name  = headerNameValuePairs[i];
            String value = headerNameValuePairs[i];
            addHeader(name, value);
        }
    }

    default void removeHeader(String name) {
        if(getHeaders() == null)
            return;

        lazyHeaders().remove(name);
    }

    default void removeHeader(String name, String value) {
        if(getHeaders() == null)
            return;

        JSList list    = (JSList)lazyHeaders().get(name);
        if (list != null) {
            list.remove(value);
            if(list.size() == 0)
                lazyHeaders().remove(name);
        }
    }
}
