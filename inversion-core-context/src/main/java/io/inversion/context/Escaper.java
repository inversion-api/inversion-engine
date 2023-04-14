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

import io.inversion.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class Escaper {
    /**
     * Escapes '\', ',' and '=' characters with a '\'
     *
     * @param string
     * @return
     */
    public static String escape(String string) {
        string = string.replace("\\", "\\\\");
        string = string.replace(",", "\\,");
        string = string.replace("=", "\\=");
        return string;
    }

    /**
     * Splits a string into chunks on '=' and ',' characters using '\' as an escape character.
     *
     * @param string
     * @return
     */
    public static List<String> unescape(String string) {
        List<String>  list    = new ArrayList<>();
        StringBuilder buff    = new StringBuilder("");
        boolean       escaped = false;
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            switch (c) {
                case '\\':
                    if (escaped) {
                        escaped = false;
                        buff.append(c);
                    } else {
                        escaped = true;
                    }
                    break;
                case '=':
                case ',':
                    if (escaped) {
                        buff.append(c);
                        escaped = false;
                    } else {
                        list.add(buff.toString());
                        buff = new StringBuilder("");
                    }
                    break;

                default:
                    if(escaped)
                        throw Utils.ex("You have an invalid escape sequence in your string. ',' and '=' '\\' are the only characters that can be escaped. You may need to add a '\\' to escape a literal '\\' in your data");

                    buff.append(c);
            }
        }
        if (buff.length() > 0)
            list.add(buff.toString());

        return list;
    }

}
