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
package io.inversion.cloud.action.elastic;

import java.util.List;
import java.util.Map;

import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.Db;
import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.Results;
import io.inversion.cloud.model.Rows.Row;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.utils.HttpUtils;
import io.inversion.cloud.utils.Utils;

public class ElasticsearchDb extends Db<ElasticsearchDb>
{
   protected static String      url                      = null;

   protected static int         maxRequestDuration       = 10;                  // duration in seconds.

   protected static final int[] allowedFailResponseCodes = {400, 401, 403, 404};

   // This is the expected maximum base query search size.  Searching beyond this value requires
   // a 'search_after' to be performed.  By default, Elastic sets this value to 10k. Typically,
   // there's no need to change this value.
   public static int            maxElasticQuerySize      = 10000;

   // When an elastic search is performed, this is the default '_source' value that will be used.
   // This value is optional and does not need to be set.
   public String                defaultSource            = null;

   public ElasticsearchDb()
   {
      withType("elasticsearch");
   }

   public ElasticsearchDb(String name)
   {
      this();
      withName(name);
   }

   //   @Override
   //   protected void startup0()
   //   {
   //      this.withType("elasticsearch");
   //
   //      reflectDb();
   //      configApi();
   //
   //      System.out.println(this);
   //   }

   public Results<Row> select(Collection table, List<Term> columnMappedTerms) throws Exception
   {
      // TODO is this an autoSuggest request or normal request?
      // see line ~78 ElasticDbGetAction

      // Apply default source when possible...adding "id", "asc" when necessary 
      ElasticsearchQuery query = new ElasticsearchQuery(table, columnMappedTerms).withCollection(table);
      if (defaultSource != null && query.getSelect().find("source") == null)
      {
         query.getSelect().withTerm("source=id");
      }

      // TODO any point in handling a 'search after' within an autosuggest search request?

      // TODO how to handle 'search after' page requests?
      // look at ElasticDbGetAction line ~122 to grasp wantedPage
      // previously, in snooze, the following parameters would be sent up when paging...
      // meta
      // prev=u - url to the previous page
      // next=v - url for the next page
      // possible params belonging to the above 'prev' 'next' fields
      // start=w - comma separated list that tells search_after where to start. [NULL] indicates an actual null value
      // prevstart=x - comma separated list that tells search_after where to the previous 'search_after' started.
      // wantedpage=y - replaces 'page/pagenum' when 'searching after' when less than ELASTIC_MAX
      // page=z - 'typically', the page to get

      // A 'start' param indicates an elastic 'search after' query should be used.
      // 'search after' queries should ONLY be used if it is believe the result 
      // will come from a row index > 10k.
      // List<String> searchAfterList = Arrays.asList(params.remove("start").split(","));

      // TODO add the json header to the request before sending to elastic 
      // List<String> headers = new ArrayList<String>(Arrays.asList("Content-Type", "application/json"));

      String url = this.url + "/" + table;
      JSNode json = query.getJson();
      Response res = null;
      if (json == null)
      {
         url += "/_search";
         res = HttpUtils.get(url).get();
      }

      Results result = new Results(query);

      if (res.isSuccess())
      {
         //         result.withRow(null);
         //         result.withRows(null);
         //         result.withNext(next)

         System.out.println(res.getContent());

         JSNode content = JSNode.parseJsonNode(res.getContent());
         JSNode hitsObj = content.getNode("hits");
         int totalHits = Integer.parseInt(hitsObj.getProperty("total").getValue().toString());

         JSArray hitsArr = hitsObj.getArray("hits");

         for (JSNode obj : hitsArr.asNodeList())
         {
            JSNode src = obj.getNode("_source");
            result.withRow(src.asMap());
         }

         result.withFoundRows(totalHits);
      }
      else
         System.out.println("request failed :*(");

      return result;
   }

   @Override
   public void delete(Collection table, List<Map<String, Object>> indexValues) throws Exception
   {
      // TODO Auto-generated method stub
   }

   @Override
   public List upsert(Collection table, List<Map<String, Object>> rows) throws Exception
   {
      return null;
   }

   private void handleAutoSuggestRequest()
   {

   }

   private void handleSearchRequest()
   {

   }

   private void handlePaging(WrappedQueryBuilder builder)
   {

   }

   //   private void reflectDb()
   //   {
   //      if (!isBootstrap())
   //      {
   //         return;
   //      }
   //
   //      try
   //      {
   //         // 'GET _all' returns all indices/aliases/mappings
   //         String url = getUrl();
   //         Response allResp = HttpUtils.get(url + "/_all").get(maxRequestDuration, TimeUnit.SECONDS);
   //
   //         if (allResp.isSuccess())
   //         {
   //            // we now have the indices, aliases for each index, and mappings (and settings if we need them)
   //
   //            JSNode jsObj = JSNode.parseJsonNode(allResp.getContent());
   //
   //            Map<String, JSNode> jsContentMap = jsObj.asMap();
   //
   //            // a map is needed when building tables to keep track of which alias'ed indexes, such as 'all', have previously been built.
   //            Map<String, Collection> tableMap = new HashMap<String, Collection>();
   //
   //            for (Map.Entry<String, JSNode> entry : jsContentMap.entrySet())
   //            {
   //               // we now have the index and with it, it's aliases and mappings
   //               buildTables(entry.getKey(), entry.getValue(), tableMap);
   //            }
   //         }
   //         else
   //         {
   //            if (allResp.getError() != null)
   //            {
   //               allResp.getError().printStackTrace();
   //               Utils.getCause(allResp.getError()).printStackTrace();
   //            }
   //            throw new ApiException(SC.matches(allResp.getStatusCode(), allowedFailResponseCodes) ? SC.SC_MAP.get(allResp.getStatusCode()) : Status.SC_500_INTERNAL_SERVER_ERROR);
   //            
   //         }
   //      }
   //      catch (Exception ex)
   //      {
   //         Utils.rethrow(ex);
   //      }
   //   }

   public void configDb() throws Exception
   {// TODO fill out
      for (String tableName : includeTables.keySet())
      {
         withCollection(buildCollection(tableName));
      }
   }

   //   private void configApi()
   //   {
   //      for (Collection t : getCollections())
   //      {
   //         List<Column> cols = t.getColumns();
   //         Collection collection = new Collection();
   //
   //         collection.withName(super.beautifyCollectionName(t.getName()));
   //         collection.withTable(t); // Entity entity = collection.withEntity(t);
   //
   //         //
   //         //         for (Column col : cols)
   //         //         {
   //         //            Attribute attr = new Attribute();
   //         //            attr.withEntity(entity);
   //         //            attr.withName(col.getName());
   //         //            attr.withColumn(col);
   //         //            attr.withHint(col.getTable().getName() + "." + col.getName());
   //         //            attr.withType(col.getType());
   //         //            entity.withAttribute(attr);
   //         //         }
   //
   //         api.withCollection(collection);
   //      }
   //   }

   protected Collection buildCollection(String tableName)
   {
      // TODO fill out
      return null;
   }

   //   /**
   //    * At the time of writing, there is no need to parse settings.
   //    * This method creates tables based on alias names of 
   //    * elastic indexes.  A table of the index will always be created
   //    * at a minimum.
   //    * 
   //    * Most tables will only have one index. An example of a 
   //    * table with multiple indexes would be the alias 'all'.
   //    * @param indexName
   //    * @param jsIndex
   //    * @return
   //    */
   //   private void buildTables(String elasticName, JSNode jsIndex, Map<String, Collection> tableMap)
   //   {
   //      Map<String, JSNode> jsMappingsDocProps = jsIndex.getNode("mappings").getNode("_doc").getNode("properties").asMap();
   //
   //      Collection table = null;
   //
   //      if (tableMap.containsKey(elasticName))
   //         table = tableMap.get(elasticName);
   //      else
   //      {
   //         table = new Collection(elasticName).withDb(this);
   //         tableMap.put(elasticName, table);
   //      }
   //
   //      withCollection(table);
   //
   //      // use the mapping to add columns to the table.
   //      addColumns(table, false, jsMappingsDocProps, "");
   //
   //      String aliasName = null;
   //      Map<String, ObjectNode> jsAliasProps = jsIndex.getNode("aliases").asMap();
   //      for (Map.Entry<String, ObjectNode> propEntry : jsAliasProps.entrySet())
   //      {
   //         aliasName = propEntry.getKey();
   //
   //         table = null;
   //
   //         // use the previously created table if it exists.
   //         if (tableMap.containsKey(aliasName))
   //            table = tableMap.get(aliasName);
   //         else
   //         {
   //            table = new Collection(aliasName).withDb(this);
   //            tableMap.put(aliasName, table);
   //         }
   //
   //         withCollection(table);
   //
   //         // use the mapping to add columns to the table.
   //         addColumns(table, false, jsMappingsDocProps, "");
   //      }
   //   }

   //   /**
   //    * @param table - add the column to this table
   //    * @param nullable - lets the column nullable
   //    * @param jsPropsMap - contains the parent's nested properties
   //    * @param parentPrefix - necessary for 'nested' column names.
   //    */
   //   private void addColumns(Collection table, boolean nullable, Map<String, JSNode> jsPropsMap, String parentPrefix)
   //   {
   //      int columnNumber = 0;
   //      for (Map.Entry<String, JSNode> propEntry : jsPropsMap.entrySet())
   //      {
   //         columnNumber += 1;
   //
   //         String colName = parentPrefix + propEntry.getKey();
   //         JSNode propValue = propEntry.getValue();
   //
   //         // potential types include: keyword, long, nested, object, boolean
   //         if (propValue.containsKey("type") && table.getColumn(colName) == null)
   //         {
   //            Column column = new Column(table, columnNumber, colName, propValue.getString("type"), true);
   //            table.withColumn(column);
   //         }
   //      }
   //   }


   public String getUrl()
   {
      return Utils.getSysEnvPropStr(getName() + ".url", url);
   }

   public ElasticsearchDb withUrl(String url)
   {
      this.url = url;
      return this;
   }
}