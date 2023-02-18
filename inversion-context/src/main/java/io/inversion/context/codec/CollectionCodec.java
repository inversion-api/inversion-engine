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
import io.inversion.context.Escaper;
import io.inversion.utils.Utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class CollectionCodec implements Codec {

    public List<Class> getTypes() {
        return Utils.add(new ArrayList<>(), Collection.class);
    }

    @Override
    public String encode(Context context, CodecPath codecPath, LinkedHashMap<String, String> props, Set<Object> encoded) {

        Collection collection = (Collection) codecPath.getBean();

        CodecPath parent = codecPath.getParent();
        while (parent != null) {
            if (parent.getBean() == collection)
                throw Utils.ex("You have an unsupported circular reference within your collection: {}");
            parent = parent.getParent();
        }

        if (codecPath.getType() == null)
            throw Utils.ex("Unknowable type. You can not encode a Collection that is not referenced by a Field because the Decoder will not be able to determine the class type during decoding.");

        Type valueType   = ((((ParameterizedType) codecPath.getType()).getActualTypeArguments())[0]);
        List encodedList = new ArrayList<>();
        int  i           = 0;
        for (Object child : collection) {
            String childKey = context.getEncoder().encode0(context, new CodecPath(codecPath, valueType, i + "", child), props, encoded);
            encodedList.add(Escaper.escape(childKey));
            i += 1;
        }

        String encodedStr = encodedList.toString();
        return encodedStr;
    }

    @Override
    public Object decode(Context context, Type type, String encoded) {
        try {
            Type       subtype    = ((((ParameterizedType) type).getActualTypeArguments())[0]);
            Collection collection = instantiateCollection(type);
            for (String part : parseList(encoded)) {
                Object value = context.getDecoder().decode(context, subtype, part);
                collection.add(value);
            }
            return collection;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public Collection instantiateCollection(Type type) {

        Class clazz = null;
        if (type instanceof ParameterizedType)
            clazz = (Class) ((ParameterizedType) type).getRawType();
        else
            clazz = (Class) type;
        try {
            return (Collection) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            //-- can't instantiate
        }

        if (List.class.isAssignableFrom(clazz))
            return new ArrayList();

        if (Set.class.isAssignableFrom(clazz))
            return new LinkedHashSet();

        throw Utils.ex("Unable to instantiate type: {}", type);
    }

    public static List<String> parseList(String string) {
        if (string.startsWith("["))
            string = string.substring(1);
        if (string.endsWith("]"))
            string = string.substring(0, string.length() - 1);

        List<String> list = Escaper.unescape(string);

        //remove the extra space that ArrayList toString added after each "," separating elements.
        for (int i = 1; i < list.size(); i++) {
            String part = list.get(i);
            if (part.startsWith(" ")) {
                part = part.substring(1);
                list.set(i, part);
            }
        }
        return list;
    }
}
