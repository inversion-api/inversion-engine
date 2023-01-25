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

package io.inversion.context.codec;

import io.inversion.context.Codec;
import io.inversion.context.CodecPath;
import io.inversion.context.Context;
import io.inversion.utils.Utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;

public class ToStringCodec implements Codec {

    List<Class> types = new ArrayList();

    public ToStringCodec() {
    }

    public ToStringCodec(Class... classes) {
        withTypes(classes);
    }

    public Codec withTypes(Class... classes) {
        for (int i = 0; classes != null && i < classes.length; i++) {
            Class clazz = classes[i];
            if (clazz != null && !this.types.contains(clazz))
                this.types.add(clazz);
        }
        return this;
    }

    public List<Class> getTypes() {
        return Collections.unmodifiableList(types);
    }

    public final String encode(Context context, CodecPath path, LinkedHashMap<String, String> props, Set<Object> encoded) {
        return toString(path.getBean());
    }


    public final Object decode(Context context, Type type, String encoded){
        return fromString(type, encoded);
    }

    public String toString(Object bean){
        return bean + "";
    }


    public Object fromString(Type type, String encoded){
        try
        {
            Class<?>       cl   = Class.forName(type.getTypeName());
            Constructor<?> cons = cl.getConstructor(String.class);
            return cons.newInstance(encoded);
        }
        catch(Exception ex){
            throw Utils.ex("Unable to instantiate/decode type {}", type);
        }
    }

}
