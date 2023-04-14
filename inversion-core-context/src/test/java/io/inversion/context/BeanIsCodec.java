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

package io.inversion.context;

import java.lang.reflect.Type;

public class BeanIsCodec implements Codec {
    String name = null;
    String property = null;

    public BeanIsCodec(){
    }

    public BeanIsCodec(String name, String property) {
        this.name = name;
        this.property = property;
    }

    @Override
    public Object decode(Context context, Type type, String encoded) {
        property = encoded.substring(13);
        return this;
    }

    @Override
    public String toString(){
        return "Hello World: " + property;
    }
}
