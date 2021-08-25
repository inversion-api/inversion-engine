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

import io.inversion.utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class Parameter {

    String        name        = null;
    String        key         = null;
    String        in          = null;
    boolean       required    = false;
    String        description = null;
    String        type    = null;
    List<Pattern> patterns = new ArrayList();
    Set<String>   regexes = new HashSet();
    int           index   = 0;

    public Parameter(){

    }

    public Parameter(String key, int index){
        withIn("path");
        withKey(key);
        withIndex(index);
        withRequired(true);
    }

    public Parameter(String name, String key, String in, boolean required){
        withName(name);
        withKey(key);
        withIn(in);
        withRequired(required);
    }

    public String toString(){
        return key + "," + index + "," + regexes;
    }

    public String getName() {
        return name;
    }

    public Parameter withName(String name) {
        this.name = name;
        return this;
    }

    public String getKey() {
        return key;
    }

    public Parameter withKey(String key) {
        this.key = key;
        return this;
    }

    public String getIn() {
        return in;
    }

    public Parameter withIn(String in) {
        this.in = in;
        return this;
    }

    public boolean isRequired() {
        return required;
    }

    public Parameter withRequired(boolean required) {
        this.required = required;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Parameter withDescription(String description) {
        this.description = description;
        return this;
    }

    public String getType() {
        return type;
    }

    public Parameter withType(String type) {
        this.type = type;
        return this;
    }

    public Set<String> getRegexs() {
        return regexes;
    }

    public Parameter withRegex(String regex) {
        if(regex != null && !regexes.contains(regex)){
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            patterns.add(pattern);
            regexes.add(regex);
        }
        return this;
    }

    public int getIndex() {
        return index;
    }

    public Parameter withIndex(int index) {
        this.index = index;
        return this;
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }

}
