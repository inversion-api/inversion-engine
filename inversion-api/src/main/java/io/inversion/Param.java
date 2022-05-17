/*
 * Copyright (c) 2015-2021 Rocket Partners, LLC
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

import java.util.*;
import java.util.regex.Pattern;

public class Param{

    public enum In {HOST, SERVER_PATH, PATH, QUERY, BODY, COOKIE, USER, CHAIN, CONTEXT, ENVIRONMENT}

    String        name        = null;
    String        key         = null;
    In            in          = null;
    boolean       required    = false;
    String        description = null;
    String        type        = "string";
    List<Pattern> patterns    = new ArrayList();
    Set<String>   regexes     = new HashSet();
    int           index       = 0;

    public Param() {

    }

    public Param(String key, int index) {
        withIn(In.PATH);
        withKey(key);
        withIndex(index);
        withRequired(true);
    }

    public Param(String name, String key, In in, boolean required) {
        withName(name);
        withKey(key);
        withIn(in);
        withRequired(required);
    }

    public String toString() {
        return "[" + in + ": {" + key + "}," + index + (regexes.size() > 0 ? (", " + regexes)  : "") + "]";
    }

    public String getName() {
        return name;
    }

    public Param withName(String name) {
        this.name = name;
        return this;
    }

    public String getKey() {
        return key;
    }

    public Param withKey(String key) {
        this.key = key;
        return this;
    }

    public In getIn() {
        return in;
    }

    public Param withIn(In in) {
        this.in = in;
        return this;
    }

    public boolean isRequired() {
        return required;
    }

    public Param withRequired(boolean required) {
        this.required = required;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Param withDescription(String description) {
        this.description = description;
        return this;
    }

    public String getType() {
        return type;
    }

    public Param withType(String type) {
        this.type = type;
        return this;
    }

    public Set<String> getRegexs() {
        return regexes;
    }

    public Param withRegex(String regex) {
        if (regex != null && !regexes.contains(regex)) {
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            patterns.add(pattern);
            regexes.add(regex);
        }
        return this;
    }

    public int getIndex() {
        return index;
    }

    public Param withIndex(int index) {
        this.index = index;
        return this;
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }

}
