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
import io.inversion.context.Context;
import io.inversion.context.CodecPath;
import io.inversion.utils.Utils;
import org.apache.commons.text.StringEscapeUtils;

import java.lang.reflect.Type;
import java.util.*;

public class PrimitiveCodec extends ToStringCodec {

    public PrimitiveCodec(){
        withTypes(String.class, //
                boolean.class, Boolean.class,
                byte.class, Byte.class, //
                char.class, Character.class, //
                short.class, Short.class, //
                int.class, Integer.class, //
                long.class, Long.class, //
                float.class, Float.class, //
                double.class, Double.class);
    }

    @Override
    public String toString(Object bean){
        if(bean == null)
            return "null";
        return StringEscapeUtils.escapeJava(bean.toString());
    }

    @Override
    public Object fromString(Type type, String encoded) {

        if("null".equalsIgnoreCase(encoded.trim()))
            return null;

        Class clazz = (Class)type;

        if (String.class.isAssignableFrom(clazz)) {
            return StringEscapeUtils.unescapeJava(encoded);
        }
        //-- trim after string to preserve trailing whitespace
        encoded = encoded.trim();
        if (boolean.class.isAssignableFrom(clazz) || Boolean.class.isAssignableFrom(clazz)) {
            encoded = encoded.toLowerCase();
            return (encoded.equals("true") || encoded.equals("t") || encoded.equals("1"));
        } else if (byte.class.isAssignableFrom(clazz) || Byte.class.isAssignableFrom(clazz)) {
            return Byte.parseByte(encoded);
        } else if (char.class.isAssignableFrom(clazz) || Character.class.isAssignableFrom(clazz)) {
            if(encoded.length() > 1)
                throw Utils.ex("You are trying to assign more than one Character to a {} field", clazz.getSimpleName());
            return encoded.charAt(0);
        } else if (short.class.isAssignableFrom(clazz) || Short.class.isAssignableFrom(clazz)) {
            return Short.parseShort(encoded);
        } else if (int.class.isAssignableFrom(clazz) || Integer.class.isAssignableFrom(clazz)) {
            return Integer.parseInt(encoded);
        } else if (long.class.isAssignableFrom(clazz) || Long.class.isAssignableFrom(clazz)) {
            return Long.parseLong(encoded);
        } else if (float.class.isAssignableFrom(clazz) || Float.class.isAssignableFrom(clazz)) {
            return Float.parseFloat(encoded);
        } else if (double.class.isAssignableFrom(clazz) || Double.class.isAssignableFrom(clazz)) {
            return Double.parseDouble(encoded);
        }

        return super.fromString(type, encoded);
    }

}
