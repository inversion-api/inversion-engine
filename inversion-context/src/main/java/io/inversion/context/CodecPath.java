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

public class CodecPath {

    CodecPath parent   = null;
    Type      type     = null;
    String    property = null;
    Object    bean     = null;

    public CodecPath(){

    }

    public CodecPath(CodecPath parent, Type type, String property, Object bean) {
        this.parent = parent;
        this.type = type;
        this.property = property;
        this.bean = bean;
    }

    public String toString(){
        String string = property != null ? property : bean.getClass().getSimpleName();
        if(parent != null)
            string = parent + "." + string;
        return string;
    }

    public CodecPath getParent() {
        return parent;
    }

    public CodecPath withParent(CodecPath parent) {
        this.parent = parent;
        return this;
    }

    public Type getType() {
        return type;
    }

    public CodecPath withType(Type type) {
        this.type = type;
        return this;
    }

    public String getProperty() {
        return property;
    }

    public CodecPath withProperty(String property) {
        this.property = property;
        return this;
    }

    public Object getBean() {
        return bean;
    }

    public CodecPath withBean(Object bean) {
        this.bean = bean;
        return this;
    }
}
