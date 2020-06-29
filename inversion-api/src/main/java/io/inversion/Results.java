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

import io.inversion.rql.Query;
import io.inversion.rql.Term;
import io.inversion.utils.Rows.Row;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Results are returned by a Db and transformed by Actions into Response content.
 * <p>
 * Dbs are not responsible for mapping from column names to json names so a Results rows
 * and terms will use column names until they are potentially transformed by an Action.
 */
public class Results<M extends Map> implements Iterable<M> {

    public static String LAST_QUERY = null;

    /**
     * the query that produced these results.
     */
    protected Query query = null;

    /**
     * The data the query produced.
     * <p>
     * Dbs are not responsible for mapping from column names to json names so these maps will
     * have column names keys when initially returned from the Db.
     * <p>
     * Actions should map them to the corresponding json property names before returning to the caller.
     */
    protected List<M> rows = new ArrayList();

    /**
     * The RQL terms that will get the next page of results the DB things there are more results.
     * <p>
     * Dbs are not responsible for mapping from column names to json names so these terms will
     * have column names when initially returned from the Db.
     * <p>
     * Actions should map the keys to the corresponding json name property names before returning to the caller.
     */
    protected List<Term> next = new ArrayList();

    /**
     * The total number of rows (if known) in the Db that match the query, not the number of rows returned in this Results.
     * <p>
     * For paginated listings, foundRows generally be greater than rows.size()
     */
    protected int    foundRows  = -1;
    protected String debugQuery = null;
    protected String testQuery  = null;

    public Results(Query query) {
        this.query = query;
    }

    public Results(Query query, int foundRows, List<M> rows) {
        this.query = query;
        this.foundRows = foundRows;
        this.rows = rows;
    }

    public Query getQuery() {
        return query;
    }

    public Results withQuery(Query query) {
        this.query = query;
        return this;
    }

    @Override
    public Iterator<M> iterator() {
        return rows.iterator();
    }

    public int size() {
        return rows.size();
    }

    public Row getRow(int index) {
        return (Row) rows.get(index);
    }

    public Results setRow(int index, M row) {
        rows.set(index, row);
        return this;
    }

    public List<M> getRows() {
        return rows;
    }

    public Results withRows(List rows) {
        this.rows = rows;
        return this;
    }

    public Results withRow(M row) {
        rows.add(row);
        return this;
    }

    public List<Term> getNext() {
        return new ArrayList(next);
    }

    public Results withNext(Term next) {
        this.next.add(next);
        return this;
    }

    public Results withNext(List<Term> next) {
        if (next != null)
            this.next.addAll(next);
        return this;
    }

    public int getFoundRows() {
        return foundRows;
    }

    public Results withFoundRows(int foundRows) {
        this.foundRows = foundRows;
        return this;
    }

    public boolean isDryRun() {
        return query.isDryRun();
    }

    public String getDebugQuery() {
        return debugQuery;
    }

    public Results withDebugQuery(String debugQuery) {
        LAST_QUERY = debugQuery;
        this.debugQuery = debugQuery;
        return this;
    }

    public String getTestQuery() {
        return testQuery;
    }

    public Results withTestQuery(String testQuery) {
        LAST_QUERY = testQuery;
        this.testQuery = testQuery;
        return this;
    }

}
