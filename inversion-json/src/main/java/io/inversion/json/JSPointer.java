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

package io.inversion.json;

public class JSPointer {
    JSPointer parent   = null;
    Object    property = null;
    JSNode    node     = null;
    String    pointer  = null;

    public JSPointer(JSPointer parent, Object property, JSNode node) {
        this.parent = parent;
        this.property = property;
        this.node = node;
        if (parent == null) {
            pointer = "";
        } else {
            pointer = parent.getPointer() + "/" + property;
        }
    }

    public JSPointer getParent() {
        return parent;
    }

    public Object getProperty() {
        return property;
    }

    public JSNode getNode() {
        return node;
    }

    public String toString() {
        return pointer;
    }

    public String getPointer() {
        return pointer;
    }

}