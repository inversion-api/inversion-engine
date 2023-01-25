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

public class MapCodec implements Codec {

    public List<Class> getTypes(){
        return Utils.add(new ArrayList<>(), Map.class);
    }

    @Override
    public String encode(Context context, CodecPath codecPath, LinkedHashMap<String, String> props, Set<Object> encoded) {

        Map map = (Map) codecPath.getBean();
        CodecPath parent = codecPath.getParent();

        try {
            while (parent != null) {
                if (parent.getBean() == map)
                    throw Utils.ex("You have an unsupported circular reference within your map: {}");
                parent = parent.getParent();
            }

            if (codecPath.getType() == null)
                throw Utils.ex("Unknowable type. You can not encode a Map that is not referenced by a Field.  During decoding, a referencing Field's generic type definition is required to determine the key and value instance types.");

            Type keyType   = ((((ParameterizedType) codecPath.getType()).getActualTypeArguments())[0]);
            Type valueType = ((((ParameterizedType) codecPath.getType()).getActualTypeArguments())[1]);

            LinkedHashMap<String, String> encodedMap = new LinkedHashMap<>();
            int                           i          = 0;
            for (Object key : map.keySet()) {
                Object value        = map.get(key);
                String encodedKey   = Escaper.escape(context.getEncoder().encode0(context, new CodecPath(codecPath, keyType, i + "", key), props, encoded));
                String encodedValue = Escaper.escape(context.getEncoder().encode0(context, new CodecPath(codecPath, valueType, (i + 1) + "", value), props, encoded));
                encodedMap.put(encodedKey, encodedValue);
                i += 2;
            }

            String encodedStr = encodedMap.toString();
            return encodedStr;
        }
        catch(Exception ex){
            throw new RuntimeException("Error decoding path '" + codecPath + "'", ex);
        }
    }

    @Override
    public Object decode(Context context, Type type, String encoded) {
        try {
            Class               rawType    = (Class) ((ParameterizedType) type).getRawType();
            Type                keyType    = ((((ParameterizedType) type).getActualTypeArguments())[0]);
            Type                valueType  = ((((ParameterizedType) type).getActualTypeArguments())[1]);
            Map                 map        = (Map) rawType.getDeclaredConstructor().newInstance();
            Map<String, String> encodedMap = parseMap(encoded);

            for (String encodedKey : encodedMap.keySet()) {
                String encodedValue = encodedMap.get(encodedKey);
                Object key          = context.getDecoder().decode(context, keyType, encodedKey);
                Object value        = context.getDecoder().decode(context, valueType, encodedValue);
                map.put(key, value);
            }
            return map;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Map<String, String> parseMap(String string) {
        if (string.startsWith("{"))
            string = string.substring(1);
        if (string.endsWith("}"))
            string = string.substring(0, string.length() - 1);

        List<String>        list = Escaper.unescape(string);

        //remove the extra space that LinkedHashMap toString added after each "," separating elements.
        for(int i=1; i<list.size(); i++){
            String part = list.get(i);
            if(part.startsWith(" ")){
                part = part.substring(1);
                list.set(i, part);
            }
        }

        Map<String, String> map      = new LinkedHashMap();
        for (int i = 0; i < list.size() - 1; i += 2) {
            map.put(list.get(i), list.get(i + 1));
        }
        return map;
    }
}
