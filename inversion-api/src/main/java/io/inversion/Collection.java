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

import io.inversion.utils.Path;
import io.inversion.utils.Rows;
import io.inversion.utils.Rows.Row;
import io.inversion.utils.Utils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.text.StringEscapeUtils;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a REST Collection and maps JSON properties property names and logical cross Collection data relationships to underlying Db tables and column names.
 * <p>
 * Api users interact with Collections and their JSON representation while Inversion abstracts the details of of the storage implementations.
 * <p>
 * Collections can remap ugly legacy column names to pretty JSON friendly camelCase names, and Collection Relationships can be used to create logical traversable
 * foreign keys between Collections with the same underlying Db or even between Collections with different backend storage systems.
 * <p>
 * Generally it is the job of a <code>Db</code> to reflect on its underlying data source and automatically configure Collections and the
 * associated Relationships that will be accessed and manipulated by Api caller.
 * <p>
 * The Engine inspects the inbound Request path and attempts to match a Collection to the call.
 * <p>
 * The default mapping would be: /${endpointPath}/[${collection}]/[${resource}]/[${relationship}]
 * <p>
 * Querying "/${endpointPath}/${collection}" would typically result an a paginated list of resources ie. rows from your underlying Db translated into JSON speak.
 * <p>
 * Querying "/${endpointPath}/${collection}/${resource}" will fetch a single resource or row.
 * <p>
 * Querying "/${endpointPath}/${collection}/${resource}/${relationship}" will fetch all members from the relationship target Collection that are related to <code>resource</code>.
 * <p>
 * RestGet/Post/Put/Patch/DeleteAction are responsible for handling basic Rest semantics for interacting with Dbs via Collections.
 * <p>
 * <p>
 * TODO: check on test cases related to hasName and path matching
 * TODO: need tests for entity keys with commas
 */
public class Collection extends Rule<Collection> implements Serializable {
    /**
     * The backend storage adapter that probably generated this Collection and associated Indexes and Relationships.
     */
    transient protected Db db = null;

    /**
     * The backend datasource name that this Collection operates on.
     * <p>
     * The tableName might be "ORDER_DETAIL" but the Collection might be named "orderDetails".
     */
    protected String tableName = null;

    /**
     * Additional names that should cause this Collection to match to a Request.
     * <p>
     * For example, in an e-commerce environment, you may overload the "orders" collection with aliases "cart", "basket", and "bag".
     */
    protected Set<String> aliases = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Properties map database column names to JSON property names.
     */
    protected ArrayList<Property> properties = new ArrayList();

    /**
     * Representation of underlying Db datasource indexes.
     */
    protected ArrayList<Index> indexes = new ArrayList();

    /**
     * Relationships like resources in one collection to the resources in another collection.
     */
    protected ArrayList<Relationship> relationships = new ArrayList();

    /**
     * Set this to true to prevent it from being automatically exposed through your Api.
     */
    protected boolean exclude = false;

    public Collection() {

    }

    public Collection(String defaultName) {
        withName(defaultName);
        withTableName(defaultName);
    }

    /**
     * @return the default collection match rule: "{_collection:" + getName() + "}/[:_resource]/[:_relationship]/*"
     * @see Request.COLLECTION_KEY
     * @see Request.RESOURCE_KEY
     * @see Request.RELATIONSHIP_KEY
     */
    @Override
    protected RuleMatcher getDefaultIncludeMatch() {
        return new RuleMatcher(null, new Path("{" + Request.COLLECTION_KEY + ":" + getName() + "}/[:" + Request.RESOURCE_KEY + "]/[:" + Request.RELATIONSHIP_KEY + "]/*"));
    }

    /**
     * Returns true if all columns are foreign keys.
     * <p>
     * In an RDBMS system, this would indicate that the table is used to link both sides
     * of a many-to-many relationship and it should NOT be a public REST Collection.
     *
     * @return the true if all columns are foreign keys.
     */
    public boolean isLinkTbl() {
        if (properties.size() == 0)
            return false;

        boolean isLinkTbl = true;
        for (Property c : properties) {
            if (!c.isFk()) {
                isLinkTbl = false;
                break;
            }
        }
        //System.out.println("IS LINK TABLE: " + name + " -> " + isLinkTbl);

        return isLinkTbl;
    }

    /**
     * Convenience overload of {@link #findProperty(String)}.
     *
     * @param jsonOrColumnName
     * @return the Property with a case insensitive json name or column name match.
     * @see #findProperty(String)
     */
    public Property getProperty(String jsonOrColumnName) {
        return findProperty(jsonOrColumnName);
    }

    /**
     * Finds the property with case insensitive jsonOrColumnName.
     * <p>
     * The algo tries to find a matching json property name first
     * before relooping over the props looking of a column name match.
     *
     * @param jsonOrColumnName
     * @return the Property with a case insensitive json name or column name match.
     */
    public Property findProperty(String jsonOrColumnName) {
        Property prop = getPropertyByJsonName(jsonOrColumnName);
        if (prop == null)
            prop = getPropertyByColumnName(jsonOrColumnName);

        return prop;
    }

    /**
     * Find the property with case insensitive jsonName
     *
     * @param name
     * @return
     */
    public Property getPropertyByJsonName(String jsonName) {
        for (Property prop : properties) {
            if (jsonName.equalsIgnoreCase(prop.getJsonName()))
                return prop;
        }
        return null;
    }

    /**
     * Find the property with case insensitive columnName
     *
     * @param name
     * @return
     */
    public Property getPropertyByColumnName(String columnName) {
        for (Property col : properties) {
            if (columnName.equalsIgnoreCase(col.getColumnName()))
                return col;
        }
        return null;
    }

    public String getColumnName(String jsonName) {
        Property prop = getPropertyByJsonName(jsonName);
        if (prop != null)
            return prop.getColumnName();
        return null;
    }

    /**
     * @return true if <code>object</code> has the same Db and name as this Collection
     */
    public boolean equals(Object object) {
        if (object == this)
            return true;

        if (object instanceof Collection) {
            Collection coll = (Collection) object;
            return (db == null || db == coll.db) && Utils.equal(getTableName(), coll.getTableName()) && Utils.equal(getName(), coll.getName());
        }
        return false;
    }

    /**
     * @return the underlying Db
     */
    public Db getDb() {
        return db;
    }

    /**
     * @param db the db to set
     */
    public Collection withDb(Db db) {
        this.db = db;
        return this;
    }

    /**
     * @return the tableName backing this Collection in the Db.
     */
    public String getTableName() {
        return tableName != null ? tableName : name;
    }

    /**
     * @param name the name to set
     */
    public Collection withTableName(String name) {
        this.tableName = name;
        return this;
    }

    /**
     * @return the name of the Collection defaulting to <code>tableName</code> if <code>name</code> is null.
     */
    @Override
    public String getName() {
        return name != null ? name : tableName;
    }

    /**
     * @return a shallow copy of <code>properties</code>
     */
    public List<Property> getProperties() {
        ArrayList props = new ArrayList(properties);
        //      Collections.sort(props);
        return props;
    }

    //   public int indexOf(Property property)
    //   {
    //      return properties.indexOf(property);
    //   }

    /**
     * Adds the property definitions to this Collection.
     * <p>
     * If there is an existing prop with a json name to json name match or a column name to column name match,
     * the new prop will not be added as it conflicts with the existing one.
     *
     * @param props
     */
    public Collection withProperties(Property... props) {
        for (Property prop : props) {
            if (getPropertyByJsonName(prop.getJsonName()) != null //
                    || getPropertyByColumnName(prop.getColumnName()) != null)
                continue;

            if (!properties.contains(prop))
                properties.add(prop);

            if (prop.getCollection() != this)
                prop.withCollection(this);
        }
        return this;
    }

    /**
     * Fluent utility method for constructing a new Property and adding it to the Collection.
     *
     * @param name the name of the Property to add
     * @param type the type of the Property to add
     * @return this
     * @see {@ io.inversion.Property(String, String)
     */
    public Collection withProperty(String name, String type) {
        return withProperty(name, type, true);
    }

    /**
     * Fluent utility method for constructing a new Property and adding it to the Collection.
     *
     * @param name     the name of the Property to add
     * @param type     the type of the Property to add
     * @param nullable is the Property nullable
     * @return this
     * @see {@ io.inversion.Property(String, String, boolean)
     */
    public Collection withProperty(String name, String type, boolean nullable) {
        return withProperties(new Property(name, type, nullable));
    }

    public void removeProperty(Property prop) {
        properties.remove(prop);
    }

    /**
     * Finds the first unique Index with the fewest number of Properties.
     *
     * @return the Index that should be treated as the primary key for the Collection
     * @see {@code io.inversion.Index.isUnique}
     */
    public Index getPrimaryIndex() {
        Index found = null;
        for (Index index : indexes) {
            if (!index.isUnique())
                continue;

            if (index.size() == 0)
                return index;

            if (found == null) {
                found = index;
            } else if (index.size() < found.size()) {
                found = index;
            }
        }
        return found;
    }

    /**
     * Gets an index by case insensitive name.
     *
     * @param indexName
     * @return the requested Index
     */
    public Index getIndex(String indexName) {
        for (Index index : indexes) {
            if (indexName.equalsIgnoreCase(index.getName()))
                return index;
        }
        return null;
    }

    /**
     * @return a shallow copy of <code>indexes</code>
     */
    public ArrayList<Index> getIndexes() {
        return new ArrayList(indexes);
    }

    public Collection withIndexes(Index... indexes) {
        for (Index index : indexes) {
            if (!this.indexes.contains(index))
                this.indexes.add(index);

            if (index.getCollection() != this)
                index.withCollection(this);
        }

        return this;
    }

    /**
     * Fluent utility method for constructing and adding a new Index.
     * <p>
     * If an Index with <code>name</code> exists it will be updated with the new information.
     * <p>
     * All of the Properties in <code>propertyNames</code> must already exist.
     *
     * @param name
     * @param type
     * @param unique
     * @param propertyNames
     * @return this
     * @see {@link io.inversion.Index(String, String, boolean, String...)}
     */
    public Collection withIndex(String name, String type, boolean unique, String... propertyNames) {
        Property[] properties = new Property[propertyNames.length];
        for (int i = 0; propertyNames != null && i < propertyNames.length; i++) {
            String   propName = propertyNames[i];
            Property prop     = getProperty(propName);
            if (prop == null) {
                System.out.println(this.properties);
                prop = getProperty(propName);
                ApiException.throw500InternalServerError("Property {} does not exist so it can't be added to the index {}", propertyNames[i], name);
            }

            properties[i] = prop;
        }

        Index index = getIndex(name);
        if (index == null) {
            index = new Index(name, type, unique, properties);
            withIndexes(index);
        } else {
            index.withType(type);
            index.withUnique(unique);
            index.withProperties(properties);
        }

        return this;
    }

    public void removeIndex(Index index) {
        indexes.remove(index);
    }

    public boolean isExclude() {
        return exclude;
    }

    public Collection withExclude(boolean exclude) {
        this.exclude = exclude;
        return this;
    }

    /**
     * @param name
     * @return the Relationship with a case insensitve name match
     */
    public Relationship getRelationship(String name) {
        for (Relationship r : relationships) {
            if (name.equalsIgnoreCase(r.getName()))
                return r;
        }
        return null;
    }

    /**
     * @return a shallow copy of <code>relationshiops</code.
     */
    public List<Relationship> getRelationships() {
        return new ArrayList(relationships);
    }

    public void removeRelationship(Relationship relationship) {
        relationships.remove(relationship);
    }

    /**
     * @param relationships the relationships to set
     * @return this
     */
    public Collection withRelationships(Relationship... relationships) {
        for (Relationship rel : relationships)
            withRelationship(rel);
        return this;
    }

    /**
     * Add a new Relationship if a Relationship with the same name does not already exist.
     *
     * @param relationship
     * @return this
     */
    public Collection withRelationship(Relationship relationship) {
        String name = relationship.getName();

        Relationship existing = name != null ? getRelationship(name) : null;
        if (existing == null) {
            if (!relationships.contains(relationship))
                relationships.add(relationship);

            if (relationship.getCollection() != this)
                relationship.withCollection(this);
        }
        return this;
    }

    /**
     * Fluent utility method to construct a Relationship and associated Indexes.
     *
     * @param childPropertyName
     * @param childFkProps      names of the existing Properties that make up the foreign key
     * @return this
     * @see #withManyToOneRelationship(Collection, String, Property...)
     */
    public Collection withManyToOneRelationship(Collection parentCollection, String childPropertyName, String... childFkProps) {
        Property[] properties = new Property[childFkProps.length];
        for (int i = 0; i < childFkProps.length; i++) {
            Property prop = getProperty(childFkProps[i]);

            if (prop == null)
                ApiException.throw500InternalServerError("Child foreign key property '{}.{}' can not be found.", getName(), childFkProps[i]);

            properties[i] = prop;
        }

        return withManyToOneRelationship(parentCollection, childPropertyName, properties);
    }

    /**
     * Fluent utility method to construct a Relationship and associated Indexes.
     * <p>
     * In addition to the new Relationship a new foreign key Index will be created from <code>childFkProps</code>
     * to <code>parentCollection</code>'s primary Index.
     *
     * @param parentCollection  the related Collection
     * @param childPropertyName what to call this relationship in the json representation of this Collection's resources.
     * @param childFkProps      this Collections Properties that are the foreign keys to <coe>
     * @return this
     */
    public Collection withManyToOneRelationship(Collection parentCollection, String childPropertyName, Property... childFkProps) {
        if (childFkProps == null || childFkProps.length == 0)
            ApiException.throw500InternalServerError("A relationship must include at least one childFkProp");

        Index fkIdx = new Index(this.getName() + "_" + Arrays.asList(childFkProps), "FOREIGN_KEY", false, childFkProps);
        withIndexes(fkIdx);

        withRelationship(new Relationship(childPropertyName, Relationship.REL_MANY_TO_ONE, this, parentCollection, fkIdx, null));

        Index primaryIdx = parentCollection.getPrimaryIndex();
        if (primaryIdx != null && childFkProps.length == primaryIdx.size()) {
            for (int i = 0; i < childFkProps.length; i++) {
                childFkProps[i].withPk(primaryIdx.getProperty(i));
            }
        }

        return this;
    }

    /**
     * Fluent utility method to construct a Relationship and associated Indexes.
     *
     * @param parentPropertyName
     * @param childCollection
     * @param childPropertyName  names of the existing Properties that make up the foreign key
     * @param childFkProps
     * @return this
     * @see #withOneToManyRelationship(String, Collection, String, Property...)
     */
    public Collection withOneToManyRelationship(String parentPropertyName, Collection childCollection, String childPropertyName, String... childFkProps) {
        Property[] properties = new Property[childFkProps.length];
        for (int i = 0; i < childFkProps.length; i++) {
            Property prop = childCollection.getProperty(childFkProps[i]);

            if (prop == null)
                ApiException.throw500InternalServerError("Child foreign key property '{}.{}' can not be found.", childCollection.getName(), childFkProps[i]);

            properties[i] = prop;
        }

        return withOneToManyRelationship(parentPropertyName, childCollection, childPropertyName, properties);
    }

    /**
     * Fluent utility method to construct a Relationship and associated Indexes.
     * <p>
     * In addition to the new Relationship a new foreign key Index will be created from <code>childFkProps</code>
     * to this Collection's primary Index.
     *
     * @param parentCollection  the related Collection
     * @param childPropertyName what to call this relationship in the json representation of this Collection's resources.
     * @param childFkProps      this Collections Properties that are the foreign keys to <coe>
     * @return this
     */
    public Collection withOneToManyRelationship(String parentPropertyName, Collection childCollection, String childPropertyName, Property... childFkProps) {
        Index fkIdx = new Index(childCollection.getName() + "_" + Arrays.asList(childFkProps), "FOREIGN_KEY", false, childFkProps);
        childCollection.withIndexes(fkIdx);

        withRelationship(new Relationship(parentPropertyName, Relationship.REL_ONE_TO_MANY, this, childCollection, fkIdx, null));
        childCollection.withRelationship(new Relationship(childPropertyName, Relationship.REL_MANY_TO_ONE, childCollection, this, fkIdx, null));

        Index primaryIdx = getPrimaryIndex();
        if (primaryIdx != null && childFkProps.length == primaryIdx.size()) {
            for (int i = 0; i < childFkProps.length; i++) {
                childFkProps[i].withPk(primaryIdx.getProperty(i));
            }
        }

        return this;
    }

    /**
     * @param tableName
     * @return true if the name or aliases patch
     */
    public boolean hasName(String nameOrAlias) {
        if (nameOrAlias == null)
            return false;

        return nameOrAlias.equalsIgnoreCase(getName()) || aliases.contains(nameOrAlias);
    }

    /**
     * @return a shallow clone of <code>aliases</code>
     */
    public Set<String> getAliases() {
        return new HashSet(aliases);
    }

    public Collection withAliases(String... aliases) {
        this.aliases.addAll(Arrays.asList(aliases));
        return this;
    }

    /**
     * Encodes the potentially multiple values of a resources primary index into a url path safe single value.
     *
     * @param values
     * @return a url safe encoding of the resources primary index values
     * @see #encodeResourceKey(Map, Index)
     */
    public String encodeResourceKey(Map<String, Object> values) {
        Index index = getPrimaryIndex();
        if (index == null)
            return null;

        return encodeResourceKey(values, index);
    }

    /**
     * Encodes the potentially multiple values of an index into a url path and query string safe single value.
     * <p>
     * In a typical REST Api configuration where you url paths might map to something like
     * "${endpoint}/${collection}/[${resource}][?{querystring}]", ${resource} is
     * the primary index of the resource that has been encoded here.
     * <p>
     * That might look like "/bookstore/books/12345" or in the case of a compound primary index
     * It might look like "/bookstore/orders/4567~abcde" where the "~" character is used to
     * separate parts of the key.
     * <p>
     * The names of the index fields are not encoded, only the values, relying on index property order to remain consistent.
     * <p>
     * This methods is used by various actions when constructing hypermedia urls that allow you to
     * uniquely identify individual resources (records in a Db) or to traverse Relationships.
     * <p>
     * The inverse of this method is {@link #decodeResourceKeys(Index, String)} which is used to
     * decode inbound Url path and query params to determine which resource is being referenced.
     *
     * @param values column name to Property value mapping for a resource
     * @param index  the index identifying the values that should be encoded
     * @return a url safe encoding of the index values separated by "~" characters or null if any of the values for an index key is null.
     * @see #encodeStr(String)
     * @see #decodeResourceKeys(Index, String)
     */
    public static String encodeResourceKey(Map values, Index index) {
        StringBuffer key = new StringBuffer("");
        for (String colName : index.getColumnNames()) {
            Object val = values.get(colName);
            if (Utils.empty(val))
                return null;

            val = encodeStr(val.toString());

            if (key.length() > 0)
                key.append("~");

            key.append(val);
        }

        return key.toString();
    }

    /**
     * Creates a "~" separated url safe concatenation of <code>pieces</code>
     *
     * @return a url safe encoding of the <code>pieces</code> separated by "~" characters
     * @see #encodeStr(String)
     * @see #encodeResourceKey(Map, Index)
     */
    public static String encodeResourceKey(List pieces) {
        StringBuffer resourceKey = new StringBuffer("");
        for (int i = 0; i < pieces.size(); i++) {
            Object piece = pieces.get(i);
            if (piece == null)
                ApiException.throw500InternalServerError("Trying to encode an resource key with a null component: '{}'.", pieces);

            resourceKey.append(decodeStr(piece.toString()));//piece.toString().replace("\\", "\\\\").replace("~", "\\~").replaceAll(",", "\\,"));
            if (i < pieces.size() - 1)
                resourceKey.append("~");
        }
        return resourceKey.toString();
    }

    public static void main(String[] args) {
        System.out.println(encodeStr("abcd/efg"));

    }

    /**
     * Encodes non url safe characters into a friendly "@FOUR_DIGIT_HEX_VALUE" equivalent that itself will not be modified by URLEncoder.encode(String).
     * <p>
     * For example, encodeing "abcd/efg" would result in "abcd@002fefg" where "@002f" is the hex encoding for "/".
     * <p>
     * While "~" characters are considered url safe, the are specifically included for encoding so that
     * {@link #decodeResourceKeys(Index, String)} can split a value on "~" before decoding its parts.
     *
     * @param string
     * @return a url safe string with non safe characters encoded as '@FOUR_DIGIT_HEX_VALUE'
     * @see https://stackoverflow.com/questions/695438/safe-characters-for-friendly-url
     * @see #encodeResourceKey(Map, Index)
     * @see #decodeResourceKeys(Index, String)
     * @see #decodeStr(String)
     */
    public static String encodeStr(String string) {
        //Pattern p = Pattern.compile("[^A-Za-z0-9]");

        Pattern p = Pattern.compile("[^A-Za-z0-9\\-\\.\\_\\(\\)\\'\\!\\:\\,\\;\\*]");
        //- . _ ~ ( ) ' ! * : @ , ;

        Matcher      m  = p.matcher(string);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String chars = m.group();
            String hex   = new String(Hex.encodeHex(chars.getBytes()));
            while (hex.length() < 4)
                hex = "0" + hex;

            //System.out.println(chars + " -> " + hex);
            m.appendReplacement(sb, "@" + hex);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * The reciprocal of {@link #encodeStr(String)} that replaces "\@[0-9a-f]{4}" hex sequences with the unescaped oritional unescaped character.
     *
     * @param string
     * @return a string with characters escaped to their hex equivalent replaced with the unescaped value.
     * @see #encodeResourceKey(Map, Index)
     * @see #decodeResourceKeys(Index, String)
     * @see #encodeStr(String)
     */
    public static String decodeStr(String string) {
        try {
            Pattern      p  = Pattern.compile("\\@[0-9a-f]{4}");
            Matcher      m  = p.matcher(string);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String group = m.group();
                String hex   = group.substring(1);
                String chars = StringEscapeUtils.unescapeJava("\\u" + hex);
                m.appendReplacement(sb, chars);
            }
            m.appendTail(sb);
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Decodes a resource key into its columnName / value parts.
     *
     * @param inKey
     * @return the decoded columnName / value pairs.
     * @see #decodeResourceKeys(Index, String)
     * @see #encodeResourceKey(Map, Index)
     */
    public Row decodeResourceKey(String inKey) {
        return decodeResourceKeys(inKey).iterator().next();
    }

    //parses val1~val2,val3~val4,val5~valc6
    public Rows decodeResourceKeys(String inKeys) {
        Index index = getPrimaryIndex();
        if (index == null)
            ApiException.throw500InternalServerError("Table '{}' does not have a unique index", this.getTableName());

        return decodeResourceKeys(index, inKeys);
    }

    /**
     * Decodes a resource key into its columnName / value parts.
     *
     * @param index identifies the columnNames by position
     * @param inKey the encoded string to decode
     * @return the decoded columnName / value pairs.
     * @see #decodeResourceKeys(Index, String)
     * @see #encodeResourceKey(Map, Index)
     */
    public Row decodeResourceKey(Index index, String inKey) {
        return decodeResourceKeys(index, inKey).iterator().next();
    }

    /**
     * Decodes a comma separated list of encoded resource keys.
     *
     * @param index  identifies the columnNames to decode
     * @param inKeys a comma separated list of encoded resource keys
     * @return a list of decoded columnName value pairs
     * @see #encodeResourceKey(Map, Index)
     * @see #encodeStr(String)
     * @see #decodeStr(String)
     */
    public Rows decodeResourceKeys(Index index, String inKeys) {
        //someone passed in the whole href...no problem, just strip it out.
        if (inKeys.startsWith("http") && inKeys.indexOf("/") > 0)
            inKeys = inKeys.substring(inKeys.lastIndexOf("/") + 1, inKeys.length());

        List colNames = index.getColumnNames();

        Rows rows = new Rows(colNames);
        for (String key : Utils.explode(",", inKeys)) {
            List row = new ArrayList();
            row.addAll(Utils.explode("~", key));

            if (row.size() != colNames.size())
                ApiException.throw400BadRequest("Supplied resource key '{}' has {} part(s) but the primary index for table '{}' has {} part(s)", row, row.size(), getTableName(), index.size());

            for (int i = 0; i < colNames.size(); i++) {
                Object value = decodeStr(row.get(i).toString());//.replace("\\\\", "\\").replace("\\~", "~").replace("\\,", ",");

                if (((String) value).length() == 0)
                    ApiException.throw400BadRequest("A key component can not be empty '{}'", inKeys);

                value = getDb().cast(index.getProperty(i), value);
                row.set(i, value);
            }
            rows.addRow(row);
        }

        return rows;
    }

    //   //parses val1~val2,val3~val4,val5~valc6
    //   public Rows decodeResourceKeys(Index index, String inKeys)
    //   {
    //      //someone passed in the whole href...no problem, just strip it out.
    //      if (inKeys.startsWith("http") && inKeys.indexOf("/") > 0)
    //         inKeys = inKeys.substring(inKeys.lastIndexOf("/") + 1, inKeys.length());
    //
    //      List colNames = index.getColumnNames();
    //
    //      Rows rows = new Rows(colNames);
    //      for (List row : parseKeys(inKeys))
    //      {
    //         if (row.size() != colNames.size())
    //            ApiException.throw400BadRequest("Supplied resource key '{}' has {} part(s) but the primary index for table '{}' has {} part(s)", row, row.size(), getTableName(), index.size());
    //
    //         for (int i = 0; i < colNames.size(); i++)
    //         {
    //            Object value = decodeStr(row.get(i).toString());//.replace("\\\\", "\\").replace("\\~", "~").replace("\\,", ",");
    //
    //            if (((String) value).length() == 0)
    //               ApiException.throw400BadRequest("A key component can not be empty '{}'", inKeys);
    //
    //            value = getDb().cast(index.getProperty(i), value);
    //            row.set(i, value);
    //         }
    //         rows.addRow(row);
    //      }
    //
    //      return rows;
    //   }
    //
    //   //parses val1~val2,val3~val4,val5~valc6
    //   public static List<List<String>> parseKeys(String inKeys)
    //   {
    //      String resourceKeys = inKeys;
    //      List<String> splits = new ArrayList();
    //
    //      List<List<String>> rows = new ArrayList();
    //
    //      boolean escaped = false;
    //      for (int i = 0; i < resourceKeys.length(); i++)
    //      {
    //         char c = resourceKeys.charAt(i);
    //         switch (c)
    //         {
    //            case '\\':
    //               escaped = !escaped;
    //               continue;
    //            case ',':
    //               if (!escaped)
    //               {
    //                  rows.add(splits);
    //                  splits = new ArrayList();
    //                  resourceKeys = resourceKeys.substring(i + 1, resourceKeys.length());
    //                  i = 0;
    //                  continue;
    //               }
    //            case '~':
    //               if (!escaped)
    //               {
    //                  splits.add(resourceKeys.substring(0, i));
    //                  resourceKeys = resourceKeys.substring(i + 1, resourceKeys.length());
    //                  i = 0;
    //                  continue;
    //               }
    //            default :
    //               escaped = false;
    //         }
    //      }
    //      if (resourceKeys.length() > 0)
    //      {
    //         splits.add(resourceKeys);
    //      }
    //
    //      if (splits.size() > 0)
    //      {
    //         rows.add(splits);
    //      }
    //
    //      for (List<String> row : rows)
    //      {
    //         for (int i = 0; i < row.size(); i++)
    //         {
    //            String value = row.get(i).replace("\\\\", "\\").replace("\\~", "~").replace("\\,", ",");
    //            row.set(i, value);
    //         }
    //      }
    //
    //      return rows;
    //   }

    /**
     * Performs a deep clone operation via object serialization/deserialization.
     * <p>
     * It is useful when you want to manually wire up numerous copies of a collection but tweak each one a bit differently.
     * <p>
     * For example, if you were connecting to a DynamoDb or CosmosDb where a single table is overloaded to support different domain objects.
     * <p>
     * This feature requires Collection, Relationship and Index to be Serializable.
     * <p>
     * The Db reference here is transient and reconnected to the clone so that this instance and the copy reference the same Db.
     *
     * @return a deep copy of this Collection referencing the same underlying Db instance.
     */
    public Collection copy() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream    oos;

            oos = new ObjectOutputStream(baos);

            oos.writeObject(this);
            oos.flush();

            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
            Collection        c   = (Collection) ois.readObject();
            c.db = this.db;
            return c;
        } catch (Exception e) {
            Utils.rethrow(e);
        }
        return null;
    }
}
