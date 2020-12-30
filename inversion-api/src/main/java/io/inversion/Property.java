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

import io.inversion.utils.Utils;

import java.io.Serializable;

/**
 *
 */
public class Property implements Serializable {
    protected String  jsonName   = null;
    protected String  columnName = null;
    protected String  type       = "string";
    protected boolean nullable   = false;
    protected boolean readOnly = false;
    protected boolean required = false;
    protected String jsonType = null;
    protected String regex = null;

    public void setCollection(Collection collection) {
        this.collection = collection;
    }

    protected String hint = null;

    protected boolean exclude = false;

    /**
     * If this Property is a foreign key, this will be populated
     * with the referenced primary key from the referred Collection
     */
    protected Property pk = null;

    protected Collection collection = null;

    public Property() {

    }

    public Property(String name) {
        this(name, "string", true);
    }

    public Property(String name, String type) {
        this(name, type, true);
    }

    public Property(String name, String type, boolean nullable) {
        withColumnName(name);
        withJsonName(name);
        withType(type);
        withNullable(nullable);
    }

//   @Override
//   public int compareTo(Property o)
//   {
//      if (o == null)
//         return 1;
//
//      if (o.collection == collection)
//      {
//         return getName().compareTo(((Property)collection.indexOf(this) > collection.indexOf(o) ? 1 : -1;
//      }
//
//      return 0;
//   }

    public boolean equals(Object object) {
        if (object == this)
            return true;

        if (object instanceof Property) {
            Property column = (Property) object;
            return ((collection == null || collection == column.collection) && Utils.equal(columnName, column.columnName));
        }
        return false;
    }

    public String toString() {
        return hint == null ? getJsonName() : hint;
    }

    /**
     * @return the primaryKey
     */
    public Property getPk() {
        return pk;
    }

    /**
     * @param primaryKey the primaryKey to set
     * @return this
     */
    public Property withPk(Property primaryKey) {
        this.pk = primaryKey;
        return this;
    }

    public boolean isFk() {
        return pk != null;
    }

    /**
     * @return the name
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * @param name the name to set
     * @return this
     */
    public Property withColumnName(String name) {
        this.columnName = name;
        return this;
    }

    /**
     * @return the name
     */
    public String getJsonName() {
        return jsonName;
    }

    /**
     * @param name the name to set
     * @return this
     */
    public Property withJsonName(String name) {
        this.jsonName = name;
        return this;
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
    public Property withType(String type) {
        if (!Utils.empty(type) && !"null".equalsIgnoreCase(type))
            this.type = type;
        return this;
    }

    /**
     * @return the collection that owns this property
     */
    public Collection getCollection() {
        return collection;
    }

    /**
     * @param collection the Collection the Property belongs to
     * @return this
     */
    public Property withCollection(Collection collection) {
        if (this.collection != collection) {
            this.collection = collection;
            collection.withProperties(this);
        }
        return this;
    }

    /**
     * @return the hint
     */
    public String getHint() {
        return hint;
    }

    /**
     * @param hint the hint to set
     * @return this
     */
    public Property withHint(String hint) {
        this.hint = hint;
        return this;
    }

    public boolean isNullable() {
        return nullable;
    }

    public Property withNullable(boolean nullable) {
        this.nullable = nullable;
        return this;
    }

    public boolean isExclude() {
        return exclude;
    }

    public Property withExclude(boolean exclude) {
        this.exclude = exclude;
        return this;
    }

    public boolean isReadOnly(){
        return readOnly;
    }

    public Property withReadOnly(boolean readOnly){
        this.readOnly = readOnly;
        return this;
    }

    public boolean isRequired(){
        return required;
    }

    public Property withRequired(boolean required){
        this.required = required;
        return this;
    }

    public Property withRegex(String regex){
        this.regex = regex;
        return this;
    }

    public String getRegex(){

        if(regex != null)
            return regex;

        String type = getJsonType();
        if("number".equalsIgnoreCase(type))
            return "[+-]?([0-9]*[.])?[0-9]+";

        return null;
    }

    public Property withJsonType(String jsonType){
        this.jsonType = jsonType;
        return this;
    }

    public String getJsonType() {

        if(jsonType != null)
            return jsonType;

        if(type == null)
            return "string";

        switch (type.toLowerCase()) {
//            case "char":
//            case "nchar":
//            case "clob":
//            case "s":
//            case "string":
//            case "varchar":
//            case "nvarchar":
//            case "longvarchar":
//            case "longnvarchar":
//            case "json":
//            case "datalink":
//            case "date":
//            case "datetime":
//            case "timestamp":
//                return "string";
            case "n":
            case "number":
            case "numeric":
            case "decimal":
            case "tinyint":
            case "smallint":
            case "integer":
            case "bigint":
            case "float":
            case "real":
            case "double":
                return "number";
            case "bool":
            case "boolean":
            case "bit":
                return "boolean";

            default:
                return "string";
        }
    }

}
