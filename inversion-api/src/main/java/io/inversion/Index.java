/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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

import io.inversion.utils.JSNode;
import io.inversion.utils.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
public class Index implements Serializable {
    protected final List<Property> properties = new ArrayList<>();
    protected       Collection     collection = null;
    protected       String         name       = null;
    protected       String         type       = null;           // primary, partition, sort, localsecondary, etc
    protected       boolean        unique     = true;

    public Index() {
        super();
    }

    public Index(String name, String type, boolean unique, Property... properties) {
        withName(name);
        withType(type);
        withUnique(unique);
        withProperties(properties);
    }

    public boolean equals(Object object) {
        if (object == this)
            return true;

        if (object instanceof Index) {
            Index index = ((Index) object);
            return ((collection == null || collection == index.collection) && Utils.equal(name, index.name));
        }
        return false;
    }

    public String toString() {
        StringBuilder buff = new StringBuilder(getCollection().getTableName()).append(".").append(name).append("(");
        for (int i = 0; i < size(); i++) {
            buff.append(getJsonName(i));
            if (i < size() - 1)
                buff.append(", ");
        }
        buff.append(")");
        return buff.toString();
    }

    public boolean isExclude() {
        if (collection.isExclude())
            return true;

        for (Property property : properties)
            if (property.isExclude())
                return true;

        return false;
    }

    public Collection getCollection() {
        return collection;
    }

    public Index withCollection(Collection coll) {
        if (this.collection != coll) {
            this.collection = coll;
            coll.withIndexes(this);
        }

        return this;
    }

    public String getName() {
        return name;
    }

    public Index withName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public Index withType(String type) {
        this.type = type;
        return this;
    }

    public boolean isType(String... types) {
        for (String type : types) {
            if (type.equalsIgnoreCase(this.type))
                return true;
        }
        return false;
    }

    public boolean isUnique() {
        return unique;
    }

    public Index withUnique(boolean unique) {
        this.unique = unique;
        return this;
    }

    public int size() {
        return properties.size();
    }

    public Index withProperties(Property... properties) {
        for (int i = 0; properties != null && i < properties.length; i++) {
            if (properties[i] != null && !this.properties.contains(properties[i]))
                this.properties.add(properties[i]);
        }
        return this;
    }

    public List<Property> getProperties() {
        return new ArrayList(properties);
    }

    public Property getProperty(int idx) {
        return properties.get(idx);
    }

    public String getColumnName(int index) {
        return index < properties.size() ? properties.get(index).getColumnName() : null;
    }

    public List<String> getColumnNames() {
        return properties.stream().map(Property::getColumnName).collect(Collectors.toList());
    }

    public String getJsonName(int index) {
        return index < properties.size() ? properties.get(index).getJsonName() : null;
    }

    public List<String> getJsonNames() {
        return properties.stream().map(Property::getJsonName).collect(Collectors.toList());
    }

//    public <T extends Map<String, Object>> T getKey(JSNode node, T key){
//        for(String name : getJsonNames()){
//            Object value = node.get(name);
//            if(value == null)
//                throw new ApiException("Key value should not be null");
//
//            key.put(name, value);
//        }
//        return key;
//    }
//
//    public  <T extends Map<String, Object>> T asForeignKey(T key){
//
//        List<Property> targetProps = new ArrayList();
//        for(Property prop : properties){
//            Property related = prop.getPk();
//            if(related == null){
//                if(targetProps.size() != 1)
//                    throw new ApiException("Can't map from primary to foreign index {}", this);
//                break;
//            }
//        }
//
//
//        return key;
//    }
//
//    public  <T extends Map<String, Object>> T asPrimaryKey(T key){
//
//        return key;
//    }

}
