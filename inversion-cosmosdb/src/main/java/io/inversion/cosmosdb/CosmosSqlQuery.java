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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.KeyValue;

import com.microsoft.azure.documentdb.Document;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.documentdb.FeedOptions;
import com.microsoft.azure.documentdb.FeedResponse;
import com.microsoft.azure.documentdb.PartitionKey;
import com.microsoft.azure.documentdb.SqlParameter;
import com.microsoft.azure.documentdb.SqlParameterCollection;
import com.microsoft.azure.documentdb.SqlQuerySpec;

import io.inversion.ApiException;
import io.inversion.Chain;
import io.inversion.Collection;
import io.inversion.Index;
import io.inversion.Results;
import io.inversion.jdbc.SqlQuery;
import io.inversion.rql.Order.Sort;
import io.inversion.rql.Term;
import io.inversion.rql.Where;
import io.inversion.utils.JSNode;

/**
 * @see https://docs.microsoft.com/en-us/azure/cosmos-db/sql-query-getting-started
 *
 */
public class CosmosSqlQuery extends SqlQuery<CosmosDb> {

   public CosmosSqlQuery() {

   }

   public CosmosSqlQuery(CosmosDb db, Collection table, List<Term> terms) {
      super(db, table, terms);
      super.withDb(db);
   }

   protected Where createWhere() {
      return new Where(this) {

         protected Term transform(Term parent) {
            if (parent.hasToken("like")) {
               String text = parent.getToken(1);
               int idx = text.indexOf("*");

               if (!(idx == 0 || idx == text.length() - 1) || idx != text.lastIndexOf("*"))
                  ApiException.throw400BadRequest("The 'like' RQL operator for CosmosDb expects a single wildcard at the beginning OR the end of a value.  CosmosDb does not really support 'like' but compatible 'like' statements are turned into 'sw' or 'ew' statments that are supported.");

               if (idx == 0) {
                  parent.withToken("ew");
                  parent.getTerm(1).withToken(text.substring(1, text.length()));
               } else {
                  parent.withToken("sw");
                  parent.getTerm(1).withToken(text.substring(0, text.length() - 1));
               }
            } else if (parent.hasToken("w", "wo")) {
               ApiException.throw400BadRequest("CosmosDb supports 'sw' and 'ew' but not 'w' or 'wo' functions.");
            }

            return super.transform(parent);
         }
      };
   }

   public Results doSelect() throws ApiException {
      Results results = new Results(this);
      CosmosDb db = getDb();

      String collectionUri = db.getCollectionUri(collection);

      String sql = getPreparedStmt();
      sql = sql.replaceAll("\r", "");
      sql = sql.replaceAll("\n", " ");

      SqlParameterCollection params = new SqlParameterCollection();
      for (int i = 0; i < values.size(); i++) {
         KeyValue kv = values.get(i);
         String varName = asVariableName(i);
         params.add(new SqlParameter(varName, kv.getValue()));
      }

      SqlQuerySpec querySpec = new SqlQuerySpec(sql, params);
      FeedOptions options = new FeedOptions();

      Object partKey = null;
      String partKeyCol = null;
      Index partKeyIdx = collection.getIndex("PartitionKey");
      if (partKeyIdx != null) {
         //-- the only way to turn cross partition querying off is to 
         //-- have a single partition key identified in your query.
         //-- If we have a pk term but it is nested in an expression
         //-- the we can't be sure the cosmos query planner can use it.

         partKey = null;
         partKeyCol = partKeyIdx.getProperty(0).getColumnName();
         Term partKeyTerm = findTerm(partKeyCol, "eq");

         if (partKeyTerm != null && partKeyTerm.getParent() == null) {
            partKey = partKeyTerm.getToken(1);
         } else if ("id".equalsIgnoreCase(partKeyCol)) {
            partKey = Chain.peek().getRequest().getResourceKey();
         }
      }

      if (partKey != null) {
         partKey = getDb().cast(partKeyIdx.getProperty(0), partKey);
         options.setEnableCrossPartitionQuery(false);
         options.setPartitionKey(new PartitionKey(partKey));
      } else {
         if (!isDryRun() && getDb() != null && !getDb().isAllowCrossPartitionQueries())
            ApiException.throw400BadRequest("CosmosSqlQuery.allowCrossPartitionQueries is false.");

         options.setEnableCrossPartitionQuery(true);
      }

      //-- for test cases and query explain
      String debug = "CosmosDb: SqlQuerySpec=" + querySpec.toJson() + " FeedOptions={enableCrossPartitionQuery=" + (partKey == null) + "}";
      debug = debug.replaceAll("\r", "");
      debug = debug.replaceAll("\n", " ");
      debug = debug.replaceAll(" +", " ");
      Chain.debug(debug);
      results.withTestQuery(debug);

      //-- end test case debug stuff

      if (!isDryRun()) {
         DocumentClient cosmos = db.getDocumentClient();
         FeedResponse<Document> queryResults = null;
         try {
            queryResults = cosmos.queryDocuments(collectionUri, querySpec, options);
         } catch (Exception ex) {
            System.err.println(ex.getMessage());
            System.err.println(debug);

            throw ex;
         }

         for (Document doc : queryResults.getQueryIterable()) {
            String json = doc.toJson();
            JSNode node = JSNode.parseJsonNode(json);

            //-- removes all cosmos applied system keys that start with "_"
            //-- TODO: might want to make this a configuration option and/or
            //-- specifically blacklist known cosmos keys as this algorithm
            //-- will delete any _ prefixed property even if it was supplied
            //-- by the user
            for (String key : node.keySet()) {
               if (key.startsWith("_"))
                  node.remove(key);
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
    * 
    * Replaces: 
    * <li>SELECT "table".* FROM "table" -> SELECT * FROM table
    * <li>"table"."column"              -> table["column"]
    * 
    * @see https://docs.microsoft.com/en-us/azure/cosmos-db/sql-query-select
    * @see https://docs.microsoft.com/en-us/azure/cosmos-db/sql-query-select#quoted-property-accessor
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
      return Arrays.asList(new Sort("id", true));
   }

   /**
    * Both offset and limit are required per cosmos spec.
    * 
    * @see https://docs.microsoft.com/en-us/azure/cosmos-db/sql-query-offset-limit
    */
   protected String printLimitClause(Parts parts, int offset, int limit) {
      if (offset < 0)
         offset = 0;

      if (limit <= 0)
         limit = 100;

      return "OFFSET " + offset + " LIMIT " + limit;
   }

   /**
    * Cosmos does not use "?" ansii sql style prepared statement vars, it uses
    * named variables prefixed with '@'. 
    */
   protected String asVariableName(int valuesPairIdx) {
      KeyValue kv = values.get(valuesPairIdx);
      return "@" + kv.getKey() + (valuesPairIdx + 1);
   }

   protected String asString(Term term) {
      String token = term.token;
      return token;
   }

   protected String printExpression(Term term, List<String> dynamicSqlChildText, List<String> preparedStmtChildText) {
      String token = term.getToken();
      StringBuffer sql = new StringBuffer("");
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
