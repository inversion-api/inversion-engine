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
package io.inversion.query;

import io.inversion.*;
import io.inversion.rql.Term;
import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a full RQL query with a SELECT,WHERE,GROUP,ORDER, and PAGE clause.
 */
public class Query<T extends Query, D extends Db, S extends Select, F extends From, W extends Where, R extends Group, O extends Order, G extends Page> extends Builder<T, T> {
    //hold ordered list of columnName=literalValue pairs
    protected final List<KeyValue> castValues     = new ArrayList<>();
    protected final List<KeyValue> originalValues = new ArrayList<>();
    protected D                    db             = null;
    protected Collection collection = null;
    protected S select = null;
    protected F from   = null;
    protected W where  = null;
    protected R group  = null;
    protected O order  = null;
    protected G page   = null;
    protected boolean dryRun = false;

    //-- OVERRIDE ME TO ADD NEW FUNCTIONALITY --------------------------
    //------------------------------------------------------------------
    //------------------------------------------------------------------

    public Query() {

    }

    public Query(D db, Collection coll) {
        this(db, coll, null);
    }

    public Query(D db, Collection coll, Object terms, String... functions) {
        super(null);
        withDb(db);
        withCollection(coll);
        withFunctions(functions);

        if (terms != null)
            withTerms(terms);
    }

    protected S createSelect() {
        return (S) new Select(this);
    }

    protected F createFrom() {
        return (F) new From(this);
    }

    protected W createWhere() {
        return (W) new Where(this);
    }

    protected R createGroup() {
        return (R) new Group(this);
    }

    protected O createOrder() {
        return (O) new Order(this);
    }

    //------------------------------------------------------------------
    //------------------------------------------------------------------

    protected G createPage() {
        return (G) new Page(this);
    }

    public T withTerm(Term term) {
        return super.withTerm(term);
    }

    public Results doSelect() throws ApiException {
        return null;
    }

    public List<Builder> getBuilders() {
        if (builders == null) {
            builders = new ArrayList<>();

            //order matters when multiple clauses can accept the same term
            getFrom();
            getWhere();
            getPage();
            getOrder();
            getGroup();
            getSelect();
        }
        return builders;
    }

    public S getSelect() {
        if (select == null) {
            select = createSelect();
            withBuilder(select);
        }
        return select;
    }

    public F getFrom() {
        if (from == null) {
            from = createFrom();
            withBuilder(from);
        }
        return from;
    }

    public W getWhere() {
        if (where == null) {
            where = createWhere();
            withBuilder(where);
        }
        return where;
    }

    public R getGroup() {
        if (group == null) {
            group = createGroup();
            withBuilder(group);
        }
        return group;
    }

    public O getOrder() {
        if (order == null) {
            order = createOrder();
            withBuilder(order);
        }
        return order;
    }

    public G getPage() {
        if (page == null) {
            page = createPage();
            withBuilder(page);
        }
        return page;
    }

    public Collection getCollection() {
        return collection;
    }

    public D getDb() {
        return db;
    }

    public T withDb(D db) {
        this.db = db;
        return r();
    }

    public T withCollection(Collection coll) {
        this.collection = coll;
        if (coll != null) {
            if (coll.getDb() != null)
                withDb((D) coll.getDb());
        }

        return r();
    }

    public int getNumValues() {
        return castValues.size();
    }

    protected T clearValues() {
        castValues.clear();
        originalValues.clear();
        return r();
    }

    protected T withColValue(String columnName, Object value) {

        originalValues.add(new DefaultKeyValue(columnName, value));

        Collection coll = this.collection;
        if (columnName != null) {
            if (columnName.contains(".")) {
                String collectionName = columnName.substring(0, columnName.indexOf("."));
                if (columnName.startsWith("~~relTbl_")) {
                    columnName = columnName.substring(columnName.indexOf("_") + 1);
                    collectionName = collectionName.substring(collectionName.indexOf("_") + 1);

                    Relationship rel = getCollection().getRelationship(collectionName);
                    if (rel != null) {
                        collectionName = rel.getRelated().getName();
                    }
                }
                coll = coll.getDb().getCollectionByTableName(collectionName);
            }

            if (coll != null) {
                String shortName = columnName.substring(columnName.indexOf(".") + 1);

                Property col = coll.getProperty(shortName);
                if (col == null)
                    throw ApiException.new500InternalServerError("Unable to find column '{}' on table '{}'", columnName, coll.getTableName());

                value = db.castJsonInput(col, value);

            }
        }

        castValues.add(new DefaultKeyValue(columnName, value));
        return r();
    }

    public List<String> getColValueKeys() {
        List keys = new ArrayList<>();
        for (KeyValue kv : castValues)
            keys.add(kv.getKey());
        return keys;
    }

    public List<Object> getColValues() {
        List keys = new ArrayList<>();
        for (KeyValue kv : castValues)
            keys.add(kv.getValue());
        return keys;
    }

    public List<Object> getOriginalValues() {
        List keys = new ArrayList<>();
        for (KeyValue kv : originalValues)
            keys.add(kv.getValue());
        return keys;
    }


    public KeyValue<String, String> getColValue(int index) {
        return castValues.get(index);
    }

    public List<KeyValue> getValues() {
        return castValues;
    }

    /**
     * Test if this query sould actually be run or just planned.
     *
     * @return true if dryRun is true or db.isDryRun is true
     */
    public boolean isDryRun() {
        return dryRun || (db != null && db.isDryRun());
    }

    public T withDryRun(boolean dryRun) {
        this.dryRun = dryRun;
        return r();
    }

}
