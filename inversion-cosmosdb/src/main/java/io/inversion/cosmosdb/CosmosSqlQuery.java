/*
 * Copyright (c) 2015-2020 Rocket Partners, LLC
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
package io.inversion.cosmosdb;

import com.microsoft.azure.documentdb.*;
import io.inversion.Index;
import io.inversion.*;
import io.inversion.jdbc.SqlQuery;
import io.inversion.json.JSMap;
import io.inversion.json.JSReader;
import io.inversion.rql.Order.Sort;
import io.inversion.rql.Term;
import io.inversion.rql.Where;
import io.inversion.utils.Utils;
import org.apache.commons.collections4.KeyValue;

import java.util.ArrayList;
import java.util.List;

/**
 * @see <a href="https://docs.microsoft.com/en-us/azure/cosmos-db/sql-query-getting-started">SQL Queries for Cosmos</a>
 */
public class CosmosSqlQuery extends SqlQuery<CosmosDb> {

    public CosmosSqlQuery(CosmosDb db, Collection table, List<Term> terms) {
        super(db, table, terms);
    }

    protected Where createWhere() {
        return new Where(this) {

            protected Term transform(Term parent) {
                if (parent.hasToken("like")) {
                    String text = parent.getToken(1);
                    int    idx  = text.indexOf("*");

                    if (!(idx == 0 || idx == text.length() - 1) || idx != text.lastIndexOf("*"))
                        throw ApiException.new400BadRequest("The 'like' RQL operator for CosmosDb expects a single wildcard at the beginning OR the end of a value.  CosmosDb does not really support 'like' but compatible 'like' statements are turned into 'sw' or 'ew' statements that are supported.");

                    if (idx == 0) {
                        parent.withToken("ew");
                        parent.getTerm(1).withToken(text.substring(1));
                    } else {
                        parent.withToken("sw");
                        parent.getTerm(1).withToken(text.substring(0, text.length() - 1));
                    }
                } else if (parent.hasToken("w", "wo")) {
                    throw ApiException.new400BadRequest("CosmosDb supports 'sw' and 'ew' but not 'w' or 'wo' functions.");
                }

                return super.transform(parent);
            }
        };
    }

    public Results doSelect() throws ApiException {
        Results results = new Results(this);
        CosmosDb        db      = getDb();

        String collectionUri = db.getCollectionUri(collection);

        String sql = getPreparedStmt();
        sql = sql.replaceAll("\r", "");
        sql = sql.replaceAll("\n", " ");

        SqlParameterCollection params = new SqlParameterCollection();
        for (int i = 0; i < values.size(); i++) {
            KeyValue kv      = values.get(i);
            String   varName = asVariableName(i);
            params.add(new SqlParameter(varName, kv.getValue()));
        }

        SqlQuerySpec querySpec = new SqlQuerySpec(sql, params);
        FeedOptions  options   = new FeedOptions();

        Object partKey    = null;
        String partKeyCol;
        Index  partKeyIdx = collection.getIndexByType(CosmosDb.INDEX_TYPE_PARTITION_KEY);
        if (partKeyIdx != null) {
            //-- the only way to turn cross partition querying off is to
            //-- have a single partition key identified in your query.
            //-- If we have a pk term but it is nested in an expression
            //-- the we can't be sure the cosmos query planner can use it.
            partKeyCol = partKeyIdx.getName();//getProperty(0).getColumnName();
            Term partKeyTerm = findTerm(partKeyCol, "eq");

            if(partKeyTerm == null) {
                //TODO: need to get all of them...but then what
                partKeyCol = partKeyIdx.getProperty(0).getColumnName();
                partKeyTerm = findTerm(partKeyCol, "eq");
            }

            if (partKeyTerm != null && partKeyTerm.getParent() == null) {
                partKey = partKeyTerm.getToken(1);
            } else if ("id".equalsIgnoreCase(partKeyCol)) {
                partKey = Chain.top().getRequest().getResourceKey();
            }
        }

        boolean partKeyMissing = false;
        if (partKey != null) {
            partKey = getDb().castJsonInput(partKeyIdx.getProperty(0), partKey);
            options.setEnableCrossPartitionQuery(false);
            options.setPartitionKey(new PartitionKey(partKey));
        } else {
            if (getDb() != null && !getDb().isAllowCrossPartitionQueries())
                partKeyMissing = true;

            options.setEnableCrossPartitionQuery(true);
        }

        //-- for test cases and query explain
        String debug = "CosmosDb: SqlQuerySpec=" + querySpec.toJson() + " FeedOptions={enableCrossPartitionQuery=" + (partKey == null) + "}";
        debug = debug.replaceAll("\r", "");
        debug = debug.replaceAll("\n", " ");
        debug = debug.replaceAll(" +", " ");
        Chain.debug(debug);
        results.withTestQuery(debug);

        System.out.println(debug);


        if (partKeyMissing)
            throw ApiException.new400BadRequest("CosmosSqlQuery.allowCrossPartitionQueries is false.");

        //-- end test case debug stuff

        if (!isDryRun()) {
            DocumentClient         cosmos = db.getDocumentClient();
            FeedResponse<Document> queryResults;
            try {
                queryResults = cosmos.queryDocuments(collectionUri, querySpec, options);
            } catch (Exception ex) {
                throw ApiException.new500InternalServerError(Utils.getCause(ex).getMessage());
            }

            for (Document doc : queryResults.getQueryIterable()) {
                String json = doc.toJson();
                JSMap  node = JSReader.asJSMap(json);

                //-- removes all cosmos applied system keys that start with "_"
                //-- TODO: might want to make this a configuration option and/or
                //-- specifically blacklist known cosmos keys as this algorithm
                //-- will delete any _ prefixed property even if it was supplied
                //-- by the user
                for (String key : node.keySet()) {
                    if (key.startsWith("_"))
                        node.removeValues(key);
                }
                //-- the JSON returned from cosmos looks crazy, keys are all jumbled up.
                node.sortKeys();
                results.withRow(node);

            }
        }

        return results;
    }

    /**
     * Makes a few blanked tweaks to the sql created by the
     * SqlQuery superclass to make it Cosmos compliant
     * <p>
     * Replaces:
     * <ul>
     * <li>SELECT "table".* FROM "table" -@gt; SELECT * FROM table
     * <li>"table"."column"              -@gt; table["column"]
     * </ul>
     *
     * @see <a href="https://docs.microsoft.com/en-us/azure/cosmos-db/sql-query-select">Cosmos Sql Query Select</a>
     * @see <a href="https://docs.microsoft.com/en-us/azure/cosmos-db/sql-query-select#quoted-property-accessor">Cosmos Sql Query - Quoted Property Accessor</a>
     */
    protected String toSql(boolean preparedStmt) {
        String sql = super.toSql(preparedStmt);

        sql = sql.replace(columnQuote + collection.getTableName() + columnQuote + ".*", "*");

        String regex = columnQuote + collection.getTableName() + columnQuote + "\\." + columnQuote + "([^" + columnQuote + "]*)" + columnQuote;
        sql = sql.replaceAll(regex, collection.getTableName() + "[\"$1\"]");

        sql = sql.replace(columnQuote + collection.getTableName() + columnQuote, collection.getTableName());

        return sql;
    }

    /**
     * The inversion configured primary index should contain at least
     * the document identifier and the partition key.  If you don't supply
     * a sort key on the query string that would default to adding two
     * fields to the sort.  If you did not configure cosmos to have a compound
     * search index, that would fail...simply solution...if you did not supply
     * a sort on the query string, just search by the "id" field.
     */
    protected List<Sort> getDefaultSorts(Parts parts) {
        return Utils.add(new ArrayList<>(), new Sort("id", true));
    }

    /**
     * Both offset and limit are required per cosmos spec.
     *
     * @see <a href="https://docs.microsoft.com/en-us/azure/cosmos-db/sql-query-offset-limit">Cosmos Offset and Limit</a>
     */
    @Override
    protected String printLimitClause(Parts parts, int offset, int limit) {
        if (offset < 0)
            offset = 0;

        if (limit <= 0)
            limit = 100;

        String clause =  "OFFSET " + offset + " LIMIT " + limit;
        parts.limit = clause;
        return clause;
    }

    /**
     * Cosmos does not use "?" ansi sql style prepared statement vars, it uses
     * named variables prefixed with '@'.
     */
    @Override
    protected String asVariableName(int valuesPairIdx) {
        KeyValue kv = values.get(valuesPairIdx);
        return "@" + kv.getKey() + (valuesPairIdx + 1);
    }

    /**
     * Overridden to exclude rdbms style '%' wildcards that are not needed for cosmos sw and ew queries.
     *
     * @return the term as a string approperiate for use in a cosmos query
     */
    @Override
    protected String asString(Term term) {

        String string = super.asString(term);
        System.out.println("String:" + string);
        Term parent = term.getParent();

        if (parent != null && string.indexOf("%") > 0 && parent.hasToken("sw", "ew")) {
            string = string.replace("%", "");
        }

        return string;
    }

    protected String printExpression(Term term, List<String> dynamicSqlChildText, List<String> preparedStmtChildText) {
        String        token = term.getToken();
        StringBuilder sql   = new StringBuilder();
        if ("n".equalsIgnoreCase(token)) {
            sql.append(" IS_NULL (");
            for (int i = 0; i < preparedStmtChildText.size(); i++) {
                sql.append(preparedStmtChildText.get(i).trim());
                if (i < preparedStmtChildText.size() - 1)
                    sql.append(" ");
            }
            sql.append(")");
        } else if ("nn".equals(token)) {
            sql.append(preparedStmtChildText.get(0)).append(" <> null");
        } else if ("sw".equalsIgnoreCase(token)) {
            sql.append(" STARTSWITH (");
            for (int i = 0; i < preparedStmtChildText.size(); i++) {
                String val = preparedStmtChildText.get(i);
                sql.append(val);
                if (i < preparedStmtChildText.size() - 1)
                    sql.append(", ");
            }
            sql.append(")");
        } else if ("ew".equalsIgnoreCase(token)) {
            sql.append(" ENDSWITH (");
            for (int i = 0; i < preparedStmtChildText.size(); i++) {
                String val = preparedStmtChildText.get(i);
                sql.append(val);
                if (i < preparedStmtChildText.size() - 1)
                    sql.append(", ");
            }
            sql.append(")");
        }
        //add other special cases here...@see SqlQuery.printExpression for examples
        else {
            return super.printExpression(term, dynamicSqlChildText, preparedStmtChildText);
        }

        return sql.toString();
    }

}
