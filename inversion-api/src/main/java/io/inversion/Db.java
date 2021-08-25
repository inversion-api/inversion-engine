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

import io.inversion.rql.RqlParser;
import io.inversion.rql.Term;
import io.inversion.utils.JSNode;
import io.inversion.utils.Path;
import io.inversion.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * An adapter to an underlying data source.
 * <p>
 * The goal of the Db abstraction is to allow Actions like DbGet/Put/Post/Patch/DeleteAction to apply the same REST CRUD operations agnostically across multiple backend data storage engines.
 * <p>
 * The primary job of a Db subclass is to:
 * <ol>
 *  <li>reflectively generate Collections to represent their underlying tables (or buckets, folders, containers etc.) columns, indexes, and relationships, during {@code #doStartup(Api)}.
 *  <li>implement REST CRUD support by implementing {@link #select(Collection, Map)}, {@link #upsert(Collection, List)}, {@link #delete(Collection, List)}.
 * </ol>
 * <p>
 * Actions such as DbGetAction then:
 * <ol>
 *  <li>translate a REST collection/resource request into columnName based json and RQL,
 *  <li>execute the requested CRUD method on the Collection's underlying Db
 *  <li>then translate the results back from the Db columnName based data into the approperiate jsonName version for external consumption.
 * </ol>
 */
public abstract class Db<T extends Db> extends Rule<T>{

    /**
     * These params are specifically NOT passed to the Query for parsing.  These are either dirty worlds like sql injection tokens or the are used by actions themselves
     */
    protected static final Set<String>           reservedParams = Collections.unmodifiableSet(new TreeSet<>(Arrays.asList("select", "insert", "update", "delete", "drop", "union", "truncate", "exec", "explain", "exclude", "expand", "collapse", "q")));
    protected final        Logger                log            = LoggerFactory.getLogger(getClass());
    /**
     * The Collections that are the REST interface to the backend tables (or buckets, folders, containers etc.) this Db exposes through an Api.
     */
    protected final        ArrayList<Collection> collections    = new ArrayList<>();
    /**
     * A tableName to collectionName map that can be used by whitelist backend tables that should be included in reflective Collection creation.
     */
    protected final        Map<String, String>   includeTables  = new HashMap<>();
    /**
     * OPTIONAL column names that should be included in RQL queries, upserts and patches.
     *
     * @see #filterOutJsonProperty(Collection, String)
     */
    protected final        Set<String>           includeColumns = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    /**
     * OPTIONAL column names that should be excluded from RQL queries, upserts and patches.
     *
     * @see #filterOutJsonProperty(Collection, String)
     */
    protected final        Set<String>           excludeColumns = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    final transient Set<Api> runningApis  = new HashSet<>();
    /**
     * Indicates that this Db should reflectively create and configure Collections to represent its underlying tables.
     * <p>
     * This would be false when an Api designer wants to very specifically configure an Api probably when the underlying db does not support the type of
     * reflection required.  For example, you may want to put specific Property and Relationship structure on top of an unstructured JSON document store.
     */
    protected       boolean  bootstrap    = true;

    /**
     * A property that can be used to disambiguate different backends supported by a single subclass.
     * <p>
     * For example type might be "mysql" for a JdbcDb.
     */
    protected       String   type         = null;
    /**
     * Used to differentiate which Collection is being referred by a Request when an Api supports Collections with the same name from different Dbs.
     */
    protected       Path     endpointPath = null;
    /**
     * When set to true the Db will do everything it can to "work offline" logging commands it would have run but not actually running them.
     */
    protected       boolean  dryRun       = false;
    transient       boolean  firstStartup = true;
    transient       boolean  shutdown     = false;

    public Db() {
    }

    public Db(String name) {
        this.name = name;
    }

    /**
     * Called by an Api to as part of Api.startup().
     * <p>
     * This implementation really only manages starting/started state, with the heaving lifting of bootstrapping delegated to {@link #doStartup(Api)}.
     *
     * @param api the api to start
     * @return this
     * @see #doStartup(Api)
     */
    public final synchronized T startup(Api api) {
        if (runningApis.contains(api))
            return (T) this;

        runningApis.add(api);
        doStartup(api);

        return (T) this;
    }

    /**
     * Made to be overridden by subclasses or anonymous inner classes to do specific init of an Api.
     * <p>
     * This method will not be called a second time after for an Api unless the Api is shutdown and then restarted.
     * <p>
     * The default implementation, when {@link #isBootstrap()} is true, calls {@link #configDb()} once globally and {@link #configApi(Api)} once for each Api passed in.
     *
     * @param api the api to start
     * @see #configDb()
     * @see #configApi(Api)
     */
    protected void doStartup(Api api) {
        try {
            if (isBootstrap()) {
                if (firstStartup) {
                    firstStartup = false;
                    configDb();
                }
                configApi(api);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Utils.rethrow(ex);
        }
    }

    /**
     * Shutsdown all running Apis.
     * <p>
     * This is primarily a method used for testing.
     *
     * @return this
     */
    public synchronized T shutdown() {
        if (!shutdown) {
            shutdown = true;
            runningApis.forEach(this::shutdown);
            doShutdown();
        }
        return (T) this;
    }

    protected void doShutdown() {

    }

    public synchronized T shutdown(Api api) {
        if (runningApis.contains(api)) {
            doShutdown(api);
            runningApis.remove(api);
        }

        if (runningApis.size() == 0)
            shutdown();

        return (T) this;
    }

    /**
     * Made to be overridden by subclasses or anonymous inner classes to do specific cleanup
     *
     * @param api the api shutting down
     */
    protected void doShutdown(Api api) {
        //default implementation does nothing, subclass can override if they need to close resources on shutdown
    }

    public boolean isRunning(Api api) {
        return runningApis.contains(api);
    }

    /**
     * Finds all records that match the supplied RQL query terms.
     * <p>
     * The implementation of this method primarily translates jsonNames to columnNames for RQL inputs and JSON outputs
     * delegating the work to {@link #doSelect(Collection, List)} where all ins and outs are based on columnName.
     *
     * @param collection the collection being queried
     * @param params     RQL terms that have been translated to use Property jsonNames
     * @return A list of maps with keys as Property jsonNames
     * @throws ApiException TODO: update/correct this javadoc
     */
    public final Results select(Collection collection, Map<String, String> params) throws ApiException {

        List<Term> terms = new ArrayList<>();

        for (String key : params.keySet()) {
            String value = params.get(key);
            Term term = RqlParser.parse(key, value);

            List<Term> illegalTerms = term.stream().filter(t -> t.isLeaf() && reservedParams.contains(t.getToken())).collect(Collectors.toList());
            if (illegalTerms.size() > 0) {
                //Chain.debug("Ignoring RQL terms with reserved tokens: " + illegalTerms);
                continue;
            }

            if (term.hasToken("eq") && term.getTerm(0).hasToken("include")) {
                //THIS IS AN OPTIMIZATION...the rest action can pull stuff OUT of the results based on
                //dotted path expressions.  If you don't use dotted path expressions the includes values
                //can be used to limit the sql select clause...however if any of the columns are actually
                //dotted paths, don't pass on to the Query the extra stuff will be removed by the rest action.
                boolean dottedInclude = false;
                for (int i = 1; i < term.size(); i++) {
                    String str = term.getToken(i);
                    if (str.contains(".")) {
                        dottedInclude = true;
                        break;
                    }
                }
                if (dottedInclude)
                    continue;

                //-- if the users requests eq(includes, href...) you have to replace "href" with the primary index column names
                for (Term child : term.getTerms()) {
                    if (child.hasToken("href") && collection != null) {
                        Index pk = collection.getPrimaryIndex();
                        if (pk != null) {
                            term.removeTerm(child);
                            for (int i = 0; i < pk.size(); i++) {
                                Property c             = pk.getProperty(i);
                                boolean  includesPkCol = false;
                                for (Term col : term.getTerms()) {
                                    if (col.hasToken(c.getColumnName())) {
                                        includesPkCol = true;
                                        break;
                                    }
                                }
                                if (!includesPkCol)
                                    term.withTerm(Term.term(term, c.getColumnName()));
                            }
                        }
                        break;
                    }
                }
            }

            terms.add(term);
        }

        //-- this sort is not strictly necessary but it makes the order of terms in generated
        //-- query text dependable so you can write better tests.
        Collections.sort(terms);

        List<Term> mappedTerms = new ArrayList<>();
        terms.forEach(term -> mappedTerms.addAll(mapToColumnNames(collection, term.copy())));

        Results results = doSelect(collection, mappedTerms);

        if (results.size() > 0) {

            for (int i = 0; i < results.size(); i++) {
                //convert the map into a JSNode
                Map<String, Object> row = results.getRow(i);

                if (collection == null) {
                    JSNode node = new JSNode(row);
                    results.setRow(i, node);
                } else {
                    JSNode node = new JSNode();
                    results.setRow(i, node);

                    //------------------------------------------------
                    //copy over defined attributes first, if the select returned
                    //extra columns they will be copied over last
                    for (Property attr : collection.getProperties()) {
                        String attrName = attr.getJsonName();
                        String colName  = attr.getColumnName();

                        boolean rowHas = row.containsKey(colName);
                        if (rowHas)
                        //if (resourceKey != null || rowHas)
                        {
                            //-- if the resourceKey was null don't create
                            //-- empty props for fields that were not
                            //-- returned from the db
                            Object val = row.remove(colName);
                            //if (!node.containsKey(attrName))
                            {
                                val = castDbOutput(attr, val);
                                node.put(attrName, val);
                            }
                        }
                    }

                    //------------------------------------------------
                    // next, if the db returned extra columns that
                    // are not mapped to attributes, just straight copy them
//                    for (String key : row.keySet()) {
//                        if (!key.equalsIgnoreCase("href") && !node.containsKey(key)) {
//                            Object value = row.get(key);
//                            node.put(key, value);
//                        }
//                    }

                    //------------------------------------------------
                    // put any primary key fields at the top of the object
                    Index idx = collection.getPrimaryIndex();
                    if(idx != null){
                        for(int j=idx.size()-1; j>=0; j--){
                            Property prop = idx.getProperty(j);
                            if(node.containsKey(prop.getJsonName()))
                                node.putFirst(prop.getJsonName(), node.get(prop.getJsonName()));
                        }
                    }

                    //if(links.size() > 0)
                    //    node.putFirst("_links", links);
                }
            }

        } // end if results.size() > 0

        //------------------------------------------------
        //the "next" params come from the db encoded with db col names
        //have to convert them to their attribute equivalents
        for (Term term : ((List<Term>) results.getNext())) {
            mapToJsonNames(collection, term);
        }

        return results;
    }

    /**
     * Finds all records that match the supplied RQL query terms.
     *
     * @param collection the collection to query
     * @param queryTerms RQL terms that have been translated to use Property columnNames not jsonNames
     * @return A list of maps with keys as Property columnNames not jsonNames
     */
    public abstract Results doSelect(Collection collection, List<Term> queryTerms) throws ApiException;

    public final List<String> upsert(Collection collection, List<Map<String, Object>> records) throws ApiException {
        return doUpsert(collection, mapToColumnNames(collection, records));
    }


    /**
     * Upserts the key/values pairs for each record into the underlying data source.
     * <p>
     * Keys that are not supplied in the call but that exist in the row in the target DB should not be modified.
     * <p>
     * Each row should minimally contain key value pairs that satisfy one of the
     * tables unique index constraints allowing an update to take place instead
     * of an insert if the row already exists in the underlying data source.
     * <p>
     * IMPORTANT #1 - implementors should note that the keys on each record may be different.
     * <p>
     * IMPORTANT #2 - strict POST/PUT vs POST/PATCH semantics are implementation specific.
     * For example, a RDBMS backed implementation may choose to upsert only the supplied
     * client supplied keys effectively making this a POST/PATCH operation.  A
     * document store that is simply storing the supplied JSON may not be able to do
     * partial updates elegantly and replace existing documents entirely rendering
     * this a POST/PUT.
     *
     * @param collection the collection being modified
     * @param records    the records being modified
     * @return the encoded resource key for every supplied row
     */
    public abstract List<String> doUpsert(Collection collection, List<Map<String, Object>> records) throws ApiException;

    /**
     * Should be called by Actions instead of upsert() only when all records are strictly known to exist.
     * <p>
     * The default implementation simply calls upsert().
     *
     * @param collection the collection to patch
     * @param records    the key/value pairs to update on existing records
     * @return the keys of all resources modified
     */
    //TODO: all rows need to be have a resourceKey
    public List<String> patch(Collection collection, List<Map<String, Object>> records) throws ApiException {
        return doPatch(collection, mapToColumnNames(collection, records));
    }

    public List<String> doPatch(Collection collection, List<Map<String, Object>> rows) throws ApiException {
        return doUpsert(collection, rows);
    }

    /**
     * Deletes rows identified by the unique index values from the underlying data source.
     * <p>
     * IMPORTANT implementors should note that the keys on each <code>indexValues</code> row may be different.
     * <p>
     * The keys should have come from a unique index, meaning that the key/value pairs
     * for each row should uniquely identify the row, however there is no guarantee
     * that each row will reference the same index.
     *
     * @param collection  the collection being modified
     * @param indexValues the identifiers for the records to delete
     */
    public final void delete(Collection collection, List<Map<String, Object>> indexValues) throws ApiException{
        doDelete(collection, mapToColumnNames(collection, indexValues));
    }

    public abstract void doDelete(Collection collection, List<Map<String, Object>> indexValues) throws ApiException;


    /**
     * Does some final configuration adds all non excluded Collections to the Api via Api.withCollection
     *
     * @param api the Api to configure.
     */
    protected void configApi(Api api) {

        for (Collection coll : getCollections()) {
            if (!coll.isExclude()) {

                //-- this is an API behavior that we want to be "automatic" that
                //-- may not be part of the data declaration.
                Index index = coll.getPrimaryIndex();
                if(index != null){
                    for(Property property : index.getProperties()){
                        property.withNullable(false);
                    }
                }
                api.withCollection(coll);
            }
        }
    }

    /**
     * Subclasses should reflectively create Collections and Properties, Indexes, and Relationships here.
     * <p>
     * The default implementation simply delegates to {@link #buildCollections()} and {@link #buildRelationships()}.
     * <p>
     * Generally, you will need to override buildCollections when implementing a new Db subclass but can probably leave buildRelationships alone
     * as all it does is reflectively build Relationships off of Indexes that are on the Collections.
     *
     * @throws ApiException when configuration fails
     */
    protected void configDb() throws ApiException {
        if (collections.size() == 0) {
            buildCollections();
            buildRelationships();
        }
        deconflictNames();
    }

    /**
     * Changes any property.jsonName that conflicts with a relationship name
     */
    protected void deconflictNames(){
        for(Collection coll : getCollections()){
            for(Relationship rel : coll.getRelationships()){
                String relName = rel.getName();
                Property conflict = coll.getPropertyByJsonName(relName);
                if(conflict != null){
                    String newName = relName;
                    Property pk = conflict.getPk();
                    if(pk != null){
                        newName += Utils.capitalize(pk.getJsonName());
                    }
                    else{
                        newName += "Id";
                    }

                    for(int i=0; i<100; i++){
                        String tempName = newName + (i==0 ? "" : i);
                        if(coll.getPropertyByJsonName(tempName) != null){
                            newName = tempName;
                            break;
                        }
                    }
                    //System.out.println("changing conflicting property name " + coll.getName() + "." + relName + " to " + newName);
                    conflict.withJsonName(newName);
                }
            }
        }
    }

    /**
     * Creates a collection for every table name in <code>includeTables</code> giving the Collections and Properties beautified JSON names.
     * <p>
     * Subclasses should override this method to reflectively create Collections, Properties and Indexes and then call super.buildCollections()
     * if they want names them beautified.
     */
    protected void buildCollections() {
        for (String tableName : includeTables.keySet()) {
            Collection collection = getCollectionByTableName(tableName);
            if (collection == null) {
                withCollection(new Collection(tableName));
            }
        }

        for (Collection coll : getCollections()) {
            if (coll.getName().equals(coll.getTableName())) {
                //collection has not already been specifically customized
                String prettyName = beautifyCollectionName(coll.getTableName());

                String pluralName = toPluralForm(prettyName);
                String singularName = toSingularForm(prettyName);

                if(pluralName.equals(singularName))//-- algo did not handle pluralization "correctly"
                    pluralName += "s";

                coll.withName(pluralName);
                coll.withSingularDispalyName(Utils.capitalize(singularName));
                coll.withPluralDisplayName(Utils.capitalize(pluralName));
            }

            for (Property prop : coll.getProperties()) {
                if (prop.getColumnName().equals(prop.getJsonName())) {
                    //-- json name has not already been specifically customized

                    String prettyName = beautifyName(prop.getColumnName());

//                    if(prop.getPk() != null) {
//                        String pkCol = prop.getPk().getColumnName();
//                        pkCol = capitalize(pkCol);
//                        prettyName += pkCol;
//
//                        for (int i = -0; i < 100; i++) {
//                            String finalName = prettyName + (i == 0 ? "" : i);
//                            if (coll.getPropertyByJsonName(finalName) != null) {
//                                prettyName = finalName;
//                                break;
//                            }
//                        }
//                    }
                    prop.withJsonName(prettyName);
                }
            }
        }
    }

    String toSingularForm(String str){
        if(str.length() > 3 && str.toLowerCase().endsWith("ies")){
            str = str.substring(0, str.length() -3);
            if(Character.isLowerCase(str.charAt(str.length()-1)))
                str += "y";
            else
                str += "Y";
        }

        if(str.toLowerCase().endsWith("s"))
            str = str.substring(0, str.length() -1);

        return str;
    }

    String toPluralForm(String str){
        if(!(str.toLowerCase().endsWith("s")))
            str = Pluralizer.plural(str);

        return str;
    }


    /**
     * Creates ONE_TO_MANY, MANY_TO_ONE, and MANY_TO_MANY Relationships with attractive RESTish names
     * based on the primary Index and foreign key Index structures of the Collections.
     * <p>
     * For all foreign key indexes, two relationships objects are created representing both sides of the relationship.
     * A MANY_TO_ONE also creates a ONE_TO_MANY and vice versa and there are always two for a MANY_TO_MANY modeling the relationship from both sides.
     */
    protected void buildRelationships() {
        for (Collection coll : getCollections()) {
            if (coll.isLinkTbl()) {
                //create reciprocal pairs for of MANY_TO_MANY relationships
                //for each pair combination in the link table.
                List<Index> indexes = coll.getIndexes();
                for (int i = 0; i < indexes.size(); i++) {
                    for (int j = 0; j < indexes.size(); j++) {
                        Index idx1 = indexes.get(i);
                        Index idx2 = indexes.get(j);

                        if (i == j || !idx1.getType().equals("FOREIGN_KEY") || !idx2.getType().equals("FOREIGN_KEY"))
                            continue;

                        Collection resource1 = idx1.getProperty(0).getPk().getCollection();
                        Collection resource2 = idx2.getProperty(0).getPk().getCollection();

                        Relationship r = new Relationship();
                        r.withType(Relationship.REL_MANY_TO_MANY);

                        r.withRelated(resource2);
                        r.withFkIndex1(idx1);
                        r.withFkIndex2(idx2);
                        r.withName(makeRelationshipName(resource1, r));
                        r.withCollection(resource1);
                    }
                }
            } else {
                for (Index fkIdx : coll.getIndexes()) {
                    try {
                        if (!fkIdx.getType().equals("FOREIGN_KEY") || fkIdx.getProperty(0).getPk() == null)
                            continue;

                        Collection pkResource = fkIdx.getProperty(0).getPk().getCollection();
                        Collection fkResource = fkIdx.getProperty(0).getCollection();

                        //ONE_TO_MANY
                        {
                            Relationship r = new Relationship();
                            //TODO:this name may not be specific enough or certain types
                            //of relationships. For example where an resource is related
                            //to another resource twice
                            r.withType(Relationship.REL_ONE_TO_MANY);
                            r.withFkIndex1(fkIdx);
                            r.withRelated(fkResource);
                            r.withName(makeRelationshipName(pkResource, r));
                            r.withCollection(pkResource);
                        }

                        //MANY_TO_ONE
                        {
                            Relationship r = new Relationship();
                            r.withType(Relationship.REL_MANY_TO_ONE);
                            r.withFkIndex1(fkIdx);
                            r.withRelated(pkResource);
                            r.withName(makeRelationshipName(fkResource, r));
                            r.withCollection(fkResource);
                        }
                    } catch (Exception ex) {
                        throw ApiException.new500InternalServerError(ex, "Error creating relationship for index: {}", fkIdx);
                    }
                }
            }
        }
    }

    /**
     * Attempts to camelCase the table name to make it an attractive REST collection name.  If <code>includeTables</code>
     * contains tableName as a key, the value from <code>includeTables</code> is returned as is.
     *
     * @param tableName the name of an underlying datasource table to be turned into a pretty REST collection name
     * @return a camelCased and pluralized version of tableName
     * @see #beautifyName(String)
     */
    protected String beautifyCollectionName(String tableName) {
        if (includeTables.containsKey(tableName))
            return includeTables.get(tableName);

        String collectionName = beautifyName(tableName);

        return collectionName;
    }

    /**
     * Try to make an attractive camelCase valid javascript variable name.
     * <p>
     * Lots of sql db designers use things like SNAKE_CASE_COLUMN_NAMES that look terrible as json property names.
     *
     * @param name the to beautify
     * @return a camelCased version of <code>name</code>
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Grammar_and_types#Variables">JSON property name</a>
     */
    protected String beautifyName(String name) {
        //all upper case...U.G.L.Y you ain't got on alibi you UGLY, hay hay you UGLY
        if (name.toUpperCase().equals(name)) {
            name = name.toLowerCase();
        }

        StringBuilder buff = new StringBuilder();

        boolean nextUpper = false;
        for (int i = 0; i < name.length(); i++) {
            char next = name.charAt(i);
            if (next == ' ' || next == '_') {
                nextUpper = true;
                continue;
            }

            if (buff.length() == 0 && //
                    !(Character.isAlphabetic(next)//
                            || next == '$'))//OK $ is a valid initial character in a JS identifier but seriously why dude, just why?
            {
                next = 'x';
            }

            if (nextUpper) {
                next = Character.toUpperCase(next);
                nextUpper = false;
            }

            if (buff.length() == 0)
                next = Character.toLowerCase(next);

            buff.append(next);
        }

        //-- special case for names like 'orderID'
        name = buff.toString();
        if(name.length() > 2 && name.endsWith("ID") && Character.isLowerCase(name.charAt(name.length()-3)))
            name = name.substring(0, name.length() -2) + "Id";

        return name;
    }

    /**
     * Attempts to construct a sensible json property name for a Relationship.
     * <p>
     * For example, a "ONE Author TO_MANY Books" relationship might show up as "books" on the Author collection and "Author" on the Books collection.
     * <p>
     * The algorithm attempts to pluralize the "MANY" side of the relationship.
     * <p>
     * The "ONE" side of a relationship uses the first foreign key column name minus an trailing case insensitive "ID", so in the above example,
     * if there is a foreign key Book.authorId pointing to the Author table, the the name would be "author".  If the foreign key column name
     * was Book.primaryAuthorKey, the relationship would be named "primaryAuthorKey".
     *
     * @param collection   the collection the relationship name being created will belong to
     * @param relationship the relationship
     * @return the json property name representing this Relationship.
     */
    protected String makeRelationshipName(Collection collection, Relationship relationship) {
        String name = null;
        String type = relationship.getType();

        String fkColName = relationship.getFk1Col1().getColumnName();
        if (fkColName.toLowerCase().endsWith("id") && fkColName.length() > 2) {
            fkColName = fkColName.substring(0, fkColName.length() - 2);
            while (fkColName.endsWith("_"))
                fkColName = fkColName.substring(0, fkColName.length() - 1);
        }
        fkColName = fkColName.trim();
        fkColName = beautifyName(fkColName);

        switch (type) {
            case Relationship.REL_MANY_TO_ONE:
                name = fkColName;
                break;
            case Relationship.REL_ONE_TO_MANY:
                //Example
                //
                //if the Alarm table has a FK to the Category table
                //this would be called to add a relationship to the Category
                //collection called "alarms"....this is the default case
                //assuming the Alarm fk column is semantically related to
                //the Category table with a name such as:
                //category, categories, categoryId or categoriesId
                //
                //say for example that the Alarm table had two foreign
                //keys to the Category table.  One called "categoryId"
                //and the other called "subcategoryId".  In this case
                //the "categoryId" column is semantically related and would
                //result in the collection property "alarms" being added
                //to the Category collection.  The "subcategoyId" column
                //name is not one of the semantically related names
                //so it results in a property called "subcategoryAlarms"
                //being added to the Category collection.

                name = relationship.getRelated().getName();

                for (Relationship aRel : collection.getRelationships()) {
                    if (relationship == aRel)
                        continue;

                    if (relationship.getRelated() == aRel.getRelated()) {
                        if (!fkColName.equals(name)) {
                            name = fkColName + Character.toUpperCase(name.charAt(0)) + name.substring(1);
                        }
                        break;
                    }
                }
                break;
            case Relationship.REL_MANY_TO_MANY:
                name = relationship.getFk2Col1().getPk().getCollection().getName();

                break;
        }

        return name;
    }

    /**
     * Casts value as Property.type
     *
     * @param property the property the value is assigned to
     * @param value    the value pulled from the DB
     * @return <code>value</code> cast to <code>Property.type</code>
     * @see Utils#castDbOutput(String, Object)
     */
    public Object castDbOutput(Property property, Object value) {
        return Utils.castDbOutput(property != null ? property.getType() : null, value);
    }

    /**
     * Casts value as Property.type.
     *
     * @param property the property the value is assigned to
     * @param value    the value to cast to the datatype of property
     * @return <code>value</code> cast to <code>Property.type</code>
     * @see Utils#castJsonInput(String, Object)
     */
    public Object castJsonInput(Property property, Object value) {
        return Utils.castJsonInput(property != null ? property.getType() : null, value);
    }

    /**
     * Casts value to as type.
     *
     * @param type  the type to cast to
     * @param value the value to cast
     * @return <code>value</code> cast to <code>type</code>
     * @see Utils#castJsonInput(String, Object)
     */
    public Object castJsonInput(String type, Object value) {
        return Utils.castJsonInput(type, value);
    }

    protected void mapToJsonNames(Collection collection, Term term) {
        if (collection == null)
            return;

        if (term.isLeaf() && !term.isQuoted()) {
            String token = term.getToken();

            Property attr = collection.findProperty(token);
            if (attr != null)
                term.withToken(attr.getJsonName());
        } else {
            for (Term child : term.getTerms()) {
                mapToJsonNames(collection, child);
            }
        }
    }

    protected Set<Term> mapToColumnNames(Collection collection, Term term) {
        Set terms = new HashSet();

        if (term.getParent() == null)
            terms.add(term);

        if (collection == null)
            return terms;

        if (term.isLeaf() && !term.isQuoted()) {
            String token = term.getToken();

            while (token.startsWith("-") || token.startsWith("+"))
                token = token.substring(1);

            StringBuilder name  = new StringBuilder();
            String[]      parts = token.split("\\.");

            //         if (parts.length > 2)//this could be a literal
            //            throw new ApiException("You can only specify a single level of relationship in dotted attributes: '" + token + "'");

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];

                if (i == parts.length - 1) {
                    Property attr = collection.findProperty(parts[i]);

                    if (attr != null)
                        name.append(attr.getColumnName());
                    else
                        name.append(parts[i]);
                } else {
                    Relationship rel = collection.getRelationship(part);

                    if (rel != null) {
                        name.append(rel.getName()).append(".");
                        collection = rel.getRelated();
                    } else {
                        name.append(parts[i]).append(".");
                    }
                }
            }

            if (!Utils.empty(name.toString())) {
                if (term.getToken().startsWith("-"))
                    name.insert(0, "-");
                term.withToken(name.toString());
            }
        } else {
            for (Term child : term.getTerms()) {
                terms.addAll(mapToColumnNames(collection, child));
            }
        }

        return terms;
    }

    protected List<Map<String, Object>> mapToColumnNames(Collection collection, List<Map<String, Object>> records){
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Map<String, Object> node : records) {
            Map<String, Object> row = new LinkedHashMap<>();
            rows.add(row);

            for (String jsonProp : node.keySet()) {
                Object value = node.get(jsonProp);
                Property collProp = collection.getProperty(jsonProp);
                if (collProp != null) {
                    value = castJsonInput(collProp, value);
                    row.put(collProp.getColumnName(), value);
                }
            }

            for (String columnName : row.keySet()) {
                //TODO can optimize?
                if (filterOutJsonProperty(collection, columnName))
                    row.remove(columnName);
            }
        }
        return rows;
    }

    protected Property getProperty(String tableName, String columnName) {
        Collection collection = getCollectionByTableName(tableName);
        if (collection != null)
            return collection.getPropertyByColumnName(columnName);

        return null;
    }

    public Collection getCollection(String collectionOrTableName) {
        for (Collection c : collections) {
            if (c.getName().equalsIgnoreCase(collectionOrTableName))
                return c;
        }

        return getCollectionByTableName(collectionOrTableName);
    }

    public Collection getCollectionByTableName(String tableName) {
        for (Collection t : collections) {
            if (tableName.equalsIgnoreCase(t.getTableName()))
                return t;
        }

        return null;
    }

    public void removeCollection(Collection table) {
        collections.remove(table);
    }

    /**
     * @return a shallow copy of <code>collections</code>
     */
    public List<Collection> getCollections() {
        return new ArrayList(collections);
    }

    /**
     * Utility that parses a comma and pipe separated list of table name to collection name mappings.
     * <p>
     * Example: db.tables=customers,books-prod-catalog|books,INVENTORY_ADJUSTED_BY_PERIOD|inventory
     * <p>
     * This method is primarily useful when an Api designer wants to manually configure Collections instead of having the Db reflectively build the Collections.
     * <p>
     * This method can also be called prior to the db reflection phase of startup to whitelist tables that the reflection should include.
     * <p>
     * The string is first split on "," to get a list of table names.
     * <p>
     * If the table name contains a "|" character, the part on the left is considered the tableName and the part on the right is considered the collectionName.
     * <p>
     * If there is no "|" then the beautified tableName is used for the collectionName.
     *
     * @param includeTables underlying data source tables that should be included as REST collections
     * @return this
     * @see #beautifyCollectionName(String)
     *
     * TODO: this should lazy init off a string config property
     */
    public T withIncludeTables(String... includeTables) {
        for(String includeTable : includeTables) {
            for (String pair : Utils.explode(",", includeTable)) {
                String tableName      = pair.indexOf('|') < 0 ? pair : pair.substring(0, pair.indexOf("|"));
                String collectionName = pair.indexOf('|') < 0 ? pair : pair.substring(pair.indexOf("|") + 1);
                withIncludeTable(tableName, collectionName);
            }
        }
        return (T) this;
    }

    /**
     * Whitelists tableName as an underlying table that should be reflectively bootstrapped and exposed as collectionName.
     *
     * @param tableName      the table to build a Collection for
     * @param collectionName the name of the target collection that can be different from tableName.
     * @return this
     */
    public T withIncludeTable(String tableName, String collectionName) {
        this.includeTables.put(tableName, collectionName);
        return (T) this;
    }

    /**
     * @param collections to include (add not replace)
     * @return this
     */
    public T withCollections(Collection... collections) {
        for (Collection collection : collections)
            withCollection(collection);

        return (T) this;
    }

    public T withCollection(Collection collection) {
        if (collection != null) {
            if (collection.getDb() != this)
                collection.withDb(this);

            if (!collections.contains(collection))
                collections.add(collection);
        }
        return (T) this;
    }

    /**
     * Checks if "collectionName.columnName" or just "columnName" is specifically included or excluded via <code>includeColumns</code>
     * <code>excludeColumns</code> or is a valid Property columnName.
     * <p>
     * This can be used to filter out things like URL path mapped parameters that don't actually match to "columns" in a document store.
     * <p>
     * This does not prevent the underlying Property from being part of a Collection object model and the names here don't actually have to be Properties.
     *
     * @param collection the collection in question
     * @param name       the name of the property to optionally filter
     * @return true if the property should be excluded
     */
    public boolean filterOutJsonProperty(Collection collection, String name) {

        String[] guesses = new String[]{name, collection.getName() + "." + name, collection.getTableName() + "." + name, collection.getTableName() + collection.getColumnName(name)};

        if (includeColumns.size() > 0 || excludeColumns.size() > 0) {
            boolean included = false;
            for (String guess : guesses) {
                if (excludeColumns.contains(guess))
                    return true;

                if (includeColumns.contains(guess))
                    included = true;
            }
            if (!included && includeColumns.size() > 0)
                return true;
        }

        return reservedParams.contains(name) || name.startsWith("_");
    }

    public T withIncludeColumns(String... columnNames) {
        includeColumns.addAll(Utils.explode(",", columnNames));
        return (T) this;
    }

    public T withExcludeColumns(String... columnNames) {
        excludeColumns.addAll(Utils.explode(",", columnNames));
        return (T) this;
    }

    public String getName() {
        return name;
    }

    /**
     * The name of the Db is used primarily for autowiring "name.property" bean props from
     *
     * @param name the name to set
     * @return this
     */
    public T withName(String name) {
        this.name = name;
        return (T) this;
    }

    public boolean isType(String... types) {
        String type = getType();
        if (type == null)
            return false;

        for (String t : types) {
            if (type.equalsIgnoreCase(t))
                return true;
        }
        return false;
    }

    public String getType() {
        return type;
    }

    public T withType(String type) {
        this.type = type;
        return (T) this;
    }

    public boolean isBootstrap() {
        return bootstrap;
    }

    public T withBootstrap(boolean bootstrap) {
        this.bootstrap = bootstrap;
        return (T) this;
    }

    public Path getEndpointPath() {
        return endpointPath;
    }

    public T withEndpointPath(Path endpointPath) {
        this.endpointPath = endpointPath;
        return (T) this;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public T withDryRun(boolean dryRun) {
        this.dryRun = dryRun;
        return (T) this;
    }

    /*
     * Copyright 2011 Atteo.
     *
     * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
     * in compliance with the License. You may obtain a copy of the License at
     *
     * http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software distributed under the License
     * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
     * or implied. See the License for the specific language governing permissions and limitations under
     * the License.
     */

    /**
     * Transforms English words from singular to plural form.
     * <p>
     * Examples:
     * <pre>
     *    English.plural("word") = "words";
     *
     *    English.plural("cat", 1) = "cat";
     *    English.plural("cat", 2) = "cats";
     * </pre>
     * </p>
     * <p>
     * Based on <a href="http://www.csse.monash.edu.au/~damian/papers/HTML/Plurals.html">
     * An Algorithmic Approach to English Pluralization</a> by Damian Conway.
     * </p>
     */
    static class Pluralizer {

        private static final String[]   CATEGORY_EX_ICES  = {"codex", "murex", "silex",};
        private static final String[]   CATEGORY_IX_ICES  = {"radix", "helix",};
        private static final String[]   CATEGORY_UM_A     = {"bacterium", "agendum", "desideratum", "erratum", "stratum", "datum", "ovum", "extremum", "candelabrum",};
        // Always us -> i
        private static final String[]   CATEGORY_US_I     = {"alumnus", "alveolus", "bacillus", "bronchus", "locus", "nucleus", "stimulus", "meniscus", "thesaurus",};
        private static final String[]   CATEGORY_ON_A     = {"criterion", "perihelion", "aphelion", "phenomenon", "prolegomenon", "noumenon", "organon", "asyndeton", "hyperbaton",};
        private static final String[]   CATEGORY_A_AE     = {"alumna", "alga", "vertebra", "persona"};
        // Always o -> os
        private static final String[]   CATEGORY_O_OS     = {"albino", "archipelago", "armadillo", "commando", "crescendo", "fiasco", "ditto", "dynamo", "embryo", "ghetto", "guano", "inferno", "jumbo", "lumbago", "magneto", "manifesto", "medico", "octavo", "photo", "pro", "quarto", "canto", "lingo", "generalissimo", "stylo", "rhino", "casino", "auto", "macro", "zero", "todo"};
        // Classical o -> i  (normally -> os)
        private static final String[]   CATEGORY_O_I      = {"solo", "soprano", "basso", "alto", "contralto", "tempo", "piano", "virtuoso",};
        private static final String[]   CATEGORY_EN_INA   = {"stamen", "foramen", "lumen"};
        // -a to -as (anglicized) or -ata (classical)
        private static final String[]   CATEGORY_A_ATA    = {"anathema", "enema", "oedema", "bema", "enigma", "sarcoma", "carcinoma", "gumma", "schema", "charisma", "lemma", "soma", "diploma", "lymphoma", "stigma", "dogma", "magma", "stoma", "drama", "melisma", "trauma", "edema", "miasma"};
        private static final String[]   CATEGORY_IS_IDES  = {"iris", "clitoris"};
        // -us to -uses (anglicized) or -us (classical)
        private static final String[]   CATEGORY_US_US    = {"apparatus", "impetus", "prospectus", "cantus", "nexus", "sinus", "coitus", "plexus", "status", "hiatus"};
        private static final String[]   CATEGORY_NONE_I   = {"afreet", "afrit", "efreet"};
        private static final String[]   CATEGORY_NONE_IM  = {"cherub", "goy", "seraph"};
        private static final String[]   CATEGORY_EX_EXES  = {"apex", "latex", "vertex", "cortex", "pontifex", "vortex", "index", "simplex"};
        private static final String[]   CATEGORY_IX_IXES  = {"appendix"};
        private static final String[]   CATEGORY_S_ES     = {"acropolis", "chaos", "lens", "aegis", "cosmos", "mantis", "alias", "dais", "marquis", "asbestos", "digitalis", "metropolis", "atlas", "epidermis", "pathos", "bathos", "ethos", "pelvis", "bias", "gas", "polis", "caddis", "glottis", "rhinoceros", "cannabis", "glottis", "sassafras", "canvas", "ibis", "trellis"};
        private static final String[]   CATEGORY_MAN_MANS = {"human", "Alabaman", "Bahaman", "Burman", "German", "Hiroshiman", "Liman", "Nakayaman", "Oklahoman", "Panaman", "Selman", "Sonaman", "Tacoman", "Yakiman", "Yokohaman", "Yuman"};
        private static       Pluralizer inflector         = new Pluralizer();
        private final        List<Rule> rules             = new ArrayList<>();

        public Pluralizer() {
            this(MODE.ENGLISH_ANGLICIZED);
        }

        public Pluralizer(MODE mode) {

            uncountable(new String[]{
                    // 2. Handle words that do not inflect in the plural (such as fish, travois, chassis, nationalities ending
                    // endings
                    "fish", "ois", "sheep", "deer", "pox", "itis",

                    // words
                    "bison", "flounder", "pliers", "bream", "gallows", "proceedings", "breeches", "graffiti", "rabies", "britches", "headquarters", "salmon", "carp", "herpes", "scissors", "chassis", "high-jinks", "sea-bass", "clippers", "homework", "series", "cod", "innings", "shears", "contretemps", "jackanapes", "species", "corps", "mackerel", "swine", "debris", "measles", "trout", "diabetes", "mews", "tuna", "djinn", "mumps", "whiting", "eland", "news", "wildebeest", "elk", "pincers",
                    "sugar"});

            // 4. Handle standard irregular plurals (mongooses, oxen, etc.)

            irregular(new String[][]{{"child", "children"}, // classical
                    {"ephemeris", "ephemerides"}, // classical
                    {"mongoose", "mongoose"}, // anglicized
                    {"mythos", "mythoi"}, // classical
                    // TO DO: handle entire word correctly
                    //{ "ox", "oxen" }, // classical
                    {"soliloquy", "soliloquies"}, // anglicized
                    {"trilby", "trilbys"}, // anglicized
                    {"genus", "genera"}, // classical
                    {"quiz", "quizzes"},});

            if (mode == MODE.ENGLISH_ANGLICIZED) {
                // Anglicized plural
                irregular(new String[][]{{"beef", "beefs"}, {"brother", "brothers"}, {"cow", "cows"}, {"genie", "genies"}, {"money", "moneys"}, {"octopus", "octopuses"}, {"opus", "opuses"},});
            } else if (mode == MODE.ENGLISH_CLASSICAL) {
                // Classical plural
                irregular(new String[][]{{"beef", "beeves"}, {"brother", "brethren"}, {"cos", "kine"}, {"genie", "genii"}, {"money", "monies"}, {"octopus", "octopodes"}, {"opus", "opera"},});
            }

            categoryRule(CATEGORY_MAN_MANS, "", "s");

            // questionable
            /*
             rule(new String[][] {
                 { "(ness)$", "$1" },
                 { "(ality)$", "$1" }
                 { "(icity)$", "$1" },
                 { "(ivity)$", "$1" },
            });
             */
            // 5. Handle irregular inflections for common suffixes
            rule(new String[][]{{"man$", "men"}, {"([lm])ouse$", "$1ice"}, {"tooth$", "teeth"}, {"goose$", "geese"}, {"foot$", "feet"}, {"zoon$", "zoa"}, {"([csx])is$", "$1es"},});

            // 6. Handle fully assimilated classical inflections
            categoryRule(CATEGORY_EX_ICES, "ex", "ices");
            categoryRule(CATEGORY_IX_ICES, "ix", "ices");
            categoryRule(CATEGORY_UM_A, "um", "a");
            categoryRule(CATEGORY_ON_A, "on", "a");
            categoryRule(CATEGORY_A_AE, "a", "ae");

            // 7. Handle classical variants of modern inflections
            if (mode == MODE.ENGLISH_CLASSICAL) {
                rule(new String[][]{{"trix$", "trices"}, {"eau$", "eaux"}, {"ieu$", "ieux"}, {"(..[iay])nx$", "$1nges"},});
                categoryRule(CATEGORY_EN_INA, "en", "ina");
                categoryRule(CATEGORY_A_ATA, "a", "ata");
                categoryRule(CATEGORY_IS_IDES, "is", "ides");
                categoryRule(CATEGORY_US_US, "", "");
                categoryRule(CATEGORY_O_I, "o", "i");
                categoryRule(CATEGORY_NONE_I, "", "i");
                categoryRule(CATEGORY_NONE_IM, "", "im");
                categoryRule(CATEGORY_EX_EXES, "ex", "ices");
                categoryRule(CATEGORY_IX_IXES, "ix", "ices");
            }

            categoryRule(CATEGORY_US_I, "us", "i");

            rule("([cs]h|[zx])$", "$1es");
            categoryRule(CATEGORY_S_ES, "", "es");
            categoryRule(CATEGORY_IS_IDES, "", "es");
            categoryRule(CATEGORY_US_US, "", "es");
            rule("(us)$", "$1es");
            categoryRule(CATEGORY_A_ATA, "", "s");

            // The suffixes -ch, -sh, and -ss all take -es in the plural (churches,
            // classes, etc)...
            rule(new String[][]{{"([cs])h$", "$1hes"}, {"ss$", "sses"}});

            // Certain words ending in -f or -fe take -ves in the plural (lives,
            // wolves, etc)...
            rule(new String[][]{{"([aeo]l)f$", "$1ves"}, {"([^d]ea)f$", "$1ves"}, {"(ar)f$", "$1ves"}, {"([nlw]i)fe$", "$1ves"}});

            // Words ending in -y take -ys
            rule(new String[][]{{"([aeiou])y$", "$1ys"}, {"y$", "ies"},});

            // Some words ending in -o take -os (including does preceded by a vowel)
            categoryRule(CATEGORY_O_I, "o", "os");
            categoryRule(CATEGORY_O_OS, "o", "os");
            rule("([aeiou])o$", "$1os");
            // The rest take -oes
            rule("o$", "oes");

            rule("ulum", "ula");

            categoryRule(CATEGORY_A_ATA, "", "es");

            rule("s$", "ses");
            // Otherwise, assume that the plural just adds -s
            rule("$", "s");
        }

        //   /**
        //    * Returns plural form of the given word.
        //    *
        //    * @param word word in singular form
        //    * @return plural form of the word
        //    */
        //   @Override
        //   public String getPlural(String word)
        //   {
        //      return super.getPlural(word);
        //   }

        /**
         * Returns plural form of the given word.
         * <p>
         * For instance:
         * <pre>
         * {@code
         * English.plural("cat") == "cats";
         * }
         * </pre>
         * </p>
         *
         * @param word word in singular form
         * @return plural form of given word
         */
        public static String plural(String word) {
            if (word.endsWith("s") || word.endsWith("S"))
                return word;

            word = inflector.getPlural(word);
            return word;
        }

        /**
         * Returns singular or plural form of the word based on count.
         * <p>
         * For instance:
         * <pre>
         * {@code
         * English.plural("cat", 1) == "cat";
         * English.plural("cat", 2) == "cats";
         * }
         * </pre>
         * </p>
         *
         * @param word  word in singular form
         * @param count word count
         * @return form of the word correct for given count
         */
        public static String plural(String word, int count) {
            return inflector.getPlural(word, count);
        }

        public static void setMode(MODE mode) {
            Pluralizer newInflector = new Pluralizer(mode);
            inflector = newInflector;
        }

        /**
         * Returns singular or plural form of the word based on count.
         *
         * @param word  word in singular form
         * @param count word count
         * @return form of the word correct for given count
         */
        public String getPlural(String word, int count) {
            if (count == 1) {
                return word;
            }
            return getPlural(word);
        }

        protected String getPlural(String word) {
            for (Rule rule : rules) {
                String result = rule.getPlural(word);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }

        protected void uncountable(String[] list) {
            rules.add(new CategoryRule(list, "", ""));
        }

        protected void irregular(String singular, String plural) {
            if (singular.charAt(0) == plural.charAt(0)) {
                rules.add(new RegExpRule(Pattern.compile("(?i)(" + singular.charAt(0) + ")" + singular.substring(1) + "$"), "$1" + plural.substring(1)));
            } else {
                rules.add(new RegExpRule(Pattern.compile(Character.toUpperCase(singular.charAt(0)) + "(?i)" + singular.substring(1) + "$"), Character.toUpperCase(plural.charAt(0)) + plural.substring(1)));
                rules.add(new RegExpRule(Pattern.compile(Character.toLowerCase(singular.charAt(0)) + "(?i)" + singular.substring(1) + "$"), Character.toLowerCase(plural.charAt(0)) + plural.substring(1)));
            }
        }

        protected void irregular(String[][] list) {
            for (String[] pair : list) {
                irregular(pair[0], pair[1]);
            }
        }

        protected void rule(String singular, String plural) {
            rules.add(new RegExpRule(Pattern.compile(singular, Pattern.CASE_INSENSITIVE), plural));
        }

        protected void rule(String[][] list) {
            for (String[] pair : list) {
                rules.add(new RegExpRule(Pattern.compile(pair[0], Pattern.CASE_INSENSITIVE), pair[1]));
            }
        }

        protected void categoryRule(String[] list, String singular, String plural) {
            rules.add(new CategoryRule(list, singular, plural));
        }

        enum MODE {
            ENGLISH_ANGLICIZED, ENGLISH_CLASSICAL
        }

        //   public static abstract class TwoFormInflector
        //   {
        private interface Rule {

            String getPlural(String singular);
        }

        private static class RegExpRule implements Rule {

            private final Pattern singular;
            private final String  plural;

            private RegExpRule(Pattern singular, String plural) {
                this.singular = singular;
                this.plural = plural;
            }

            @Override
            public String getPlural(String word) {
                StringBuffer buffer  = new StringBuffer();
                Matcher      matcher = singular.matcher(word);
                if (matcher.find()) {
                    matcher.appendReplacement(buffer, plural);
                    matcher.appendTail(buffer);
                    return buffer.toString();
                }
                return null;
            }
        }

        private static class CategoryRule implements Rule {

            private final String[] list;
            private final String   singular;
            private final String   plural;

            public CategoryRule(String[] list, String singular, String plural) {
                this.list = list;
                this.singular = singular;
                this.plural = plural;
            }

            @Override
            public String getPlural(String word) {
                String lowerWord = word.toLowerCase();
                for (String suffix : list) {
                    if (lowerWord.endsWith(suffix)) {
                        if (!lowerWord.endsWith(singular)) {
                            throw new RuntimeException("Internal error");
                        }
                        return word.substring(0, word.length() - singular.length()) + plural;
                    }
                }
                return null;
            }
        }
    }

}
