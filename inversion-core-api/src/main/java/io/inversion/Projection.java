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

package io.inversion;

import java.util.ArrayList;
import java.util.List;

public class Projection {

    //ALL("ALL"),
    //KEYS_ONLY("KEYS_ONLY"),
    //INCLUDE("INCLUDE");

    String              type           = "KEYS_ONLY";
    ArrayList<Property> properties = new ArrayList();

    public Projection withProperty(Property property){
        if(property != null && !properties.contains(property)){
            properties.add(property);
        }
        return this;
    }

    public Projection withProperties(Property... properties){
        for(int i=0; properties != null && i<properties.length; i++){
            if(properties[i] != null){
                withProperty(properties[i]);
            }
        }
        return this;
    }

    public String getType() {
        return type;
    }

    public Projection withType(String type) {
        this.type = type;
        return this;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public Projection withProperties(List<Property> properties) {
        this.properties = new ArrayList(properties);
        return this;
    }
}
