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

public class JSPath {
    JSPath path     = null;
    Object property = null;
    JSNode node     = null;

    public JSPath(JSPath path, Object property, JSNode node) {
        this.path = path;
        this.property = property;
        this.node = node;
    }

    public JSPath getParent() {
        return path;
    }

    public Object getProperty() {
        return property;
    }

    public JSNode getNode() {
        return node;
    }
}