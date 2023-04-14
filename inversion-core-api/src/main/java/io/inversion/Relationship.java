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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Relationship implements Serializable {

    public static final String REL_ONE_TO_ONE_PARENT = "ONE_TO_ONE_PARENT";
    public static final String REL_ONE_TO_ONE_CHILD = "ONE_TO_ONE_CHILD";

    public static final String REL_MANY_TO_ONE  = "MANY_TO_ONE";
    public static final String REL_ONE_TO_MANY  = "ONE_TO_MANY";
    public static final String REL_MANY_TO_MANY = "MANY_TO_MANY";


    protected String name     = null;
    protected String type     = null;
    protected Index  fkIndex1 = null;
    protected Index  fkIndex2 = null;

    protected Collection collection = null;
    protected Collection related    = null;

    protected boolean exclude = false;

    public Relationship() {

    }

    public Relationship(String name, String type, Collection collection, Collection related, Index fkIndex1, Index fkIndex2) {
        withName(name);
        withType(type);
        withCollection(collection);
        withRelated(related);
        withFkIndex1(fkIndex1);
        withFkIndex2(fkIndex2);
    }

    public boolean isExclude() {
        return exclude || (fkIndex1 != null && fkIndex1.isExclude()) || (fkIndex2 != null && fkIndex2.isExclude());
    }

    public Relationship withExclude(boolean exclude) {
        this.exclude = exclude;
        return this;
    }

    /**
     * @return the collection
     */
    public Collection getCollection() {
        return collection;
    }

    public Relationship withCollection(Collection collection) {
        if (this.collection != collection) {
            this.collection = collection;
            if (collection != null)
                collection.withRelationship(this);
        }
        return this;
    }

    /**
     * @return the related
     */
    public Collection getRelated() {
        return related;
    }

    public Relationship getInverse() {
        if (isManyToMany()) {
            for (Relationship other : related.getRelationships()) {
                if (other == this)
                    continue;

                if (!other.isManyToMany())
                    continue;

                if (getFkIndex1().equals(other.getFkIndex2())) {
                    return other;
                }
            }
        } else {
            for (Relationship other : related.getRelationships()) {
                if (other == this)
                    continue;

                if (isManyToOne() && !other.isOneToMany())
                    continue;

                if (isManyToMany() && !other.isManyToOne())
                    continue;

                if (isOneToOneParent() && !other.isOneToOneChild())
                    continue;

                if (isOneToOneChild() && !other.isOneToOneParent())
                    continue;

                if (getFkIndex1().equals(other.getFkIndex1()) //
                        && getPrimaryKeyTable1().getResourceIndex().equals(other.getPrimaryKeyTable1().getResourceIndex())) {
                    return other;
                }
            }
        }

        return null;
    }

    /**
     * @param related the related to set
     * @return this
     */
    public Relationship withRelated(Collection related) {
        this.related = related;
        return this;
    }

    public boolean isManyToMany() {
        return REL_MANY_TO_MANY.equalsIgnoreCase(type);
    }

    public boolean isOneToMany() {
        return REL_ONE_TO_MANY.equalsIgnoreCase(type);
    }

    public boolean isManyToOne() {
        return REL_MANY_TO_ONE.equalsIgnoreCase(type);
    }

    public boolean isOneToOneParent() {
        return REL_ONE_TO_ONE_PARENT.equalsIgnoreCase(type);
    }

    public boolean isOneToOneChild() {
        return REL_ONE_TO_ONE_CHILD.equalsIgnoreCase(type);
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     * @return this
     */
    public Relationship withName(String name) {
        this.name = name;
        return this;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Relationship))
            return false;

        if (obj == this)
            return true;

        return toString().equals(obj.toString());
    }

    public String toString() {
        try {
            String str = collection.getName() + "." + getName() + " : " + getType() + " ";

            if (isManyToOne()) {
                str += getFkIndex1() + " -> " + getRelated().getResourceIndex();
            } else if (isOneToMany()) {
                str += collection.getResourceIndex() + " <- " + getFkIndex1();
            } else {
                str += getFkIndex1() + " <--> " + getFkIndex2();
            }

            return str;
        } catch (NullPointerException ex) {
            return "Relationship: " + name + "-" + type + "-" + fkIndex1 + "-" + fkIndex2;
        }
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     * @return this
     */
    public Relationship withType(String type) {
        this.type = type;
        return this;
    }

    public Index getFkIndex1() {
        return fkIndex1;
    }

    public Relationship withFkIndex1(Index fkIndex1) {
        this.fkIndex1 = fkIndex1;
        return this;
    }

    public Index getFkIndex2() {
        return fkIndex2;
    }

    public Relationship withFkIndex2(Index fkIndex2) {
        this.fkIndex2 = fkIndex2;
        return this;
    }

    public Collection getPrimaryKeyTable1() {
        return fkIndex1.getProperty(0).getCollection();
    }

    /**
     * @return the fkCol1
     */
    public Property getFk1Col1() {
        return fkIndex1.getProperty(0);
    }

    /**
     * @return the fkCol2
     */
    public Property getFk2Col1() {
        return fkIndex2.getProperty(0);
    }


    public Map<String, Object> buildPrimaryKeyFromForeignKey(Map<String, Object> foreignKey) {

        if(!isManyToOne())
            throw new ApiException("unsupported");

        Map<String, Object> primaryKey = new LinkedHashMap<>();

        Index fkIdx = getFkIndex1();
        Index pkIdx = getRelated().getResourceIndex();

        if(fkIdx.size() == 1 && pkIdx.size() > 1){
            //-- this is compressed foreign key and a composite primary key

            Object compressedFk = foreignKey.get(fkIdx.getProperty(0));
            if(!(compressedFk instanceof String))
                return null;

            Map decoded = getRelated().decodeKeyToJsonNames((String)compressedFk);
            if(decoded == null)
                return null;

            primaryKey.putAll(decoded);

        }else if(fkIdx.size() > 1 && pkIdx.size() > 0){
            //-- this is compressed primary key and a composite foreign key
            String compressedPk= related.encodeKey(foreignKey, getFkIndex1(), true);
            if(compressedPk == null)
                return null;
            primaryKey.put(fkIdx.getJsonName(0), compressedPk);


        }else if(fkIdx.size() == pkIdx.size()){
            for(int i=0; i<fkIdx.size(); i++){
                Property prop = fkIdx.getProperty(i);
                String pkProp = prop.getPk().getJsonName();
                Object value = foreignKey.get(prop.getJsonName());
                if(value == null)
                    return null;
                primaryKey.put(pkProp, value);
            }
        }else{
            throw new ApiException("Unable to map between indexes.");
        }

        return primaryKey;
    }

    public Map<String, Object> buildForeignKeyFromPrimaryKey(Map<String, Object> primaryKey){

        if(!isManyToOne())
            throw new ApiException("unsupported");

        Map<String, Object> foreignKey = new LinkedHashMap<>();

        Index fkIdx = getFkIndex1();
        Index pkIdx = getRelated().getResourceIndex();

        if(fkIdx.size() == 1 && pkIdx.size() > 1){
            //-- this is compressed foreign key and a composite primary key
            String compressedFk = getRelated().encodeKey(primaryKey, getRelated().getResourceIndex(), true);
            if(compressedFk == null)
                return null;
            foreignKey.put(fkIdx.getJsonName(0), compressedFk);
        }else if(fkIdx.size() > 1 && pkIdx.size() == 0){
            //-- this is compressed primary key and a composite foreign key
            //TODO: map a compressed primary key to a composite foreign key
        }else if(fkIdx.size() == pkIdx.size()){
            for(int i=0; i<fkIdx.size(); i++){
                Property prop = fkIdx.getProperty(i);
                if(prop == null || prop.getPk() == null)
                    System.out.println("what?");
                Object value = primaryKey.get(prop.getPk().getJsonName());
                if(value == null)
                    return null;
                foreignKey.put(prop.getJsonName(), value);
            }
        }else{
            throw new ApiException("Unable to map between indexes.");
        }

        return foreignKey;
    }


}
