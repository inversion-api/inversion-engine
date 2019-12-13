/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.action.elastic.v03x;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.inversion.cloud.action.elastic.v03x.dsl.ElasticRql;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.Column;
import io.inversion.cloud.model.Db;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.Results;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.model.Table;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.utils.FutureResponse;
import io.inversion.cloud.utils.HttpUtils;
import io.inversion.cloud.utils.Rows.Row;
import io.inversion.cloud.utils.Utils;

public class ElasticDb extends Db<ElasticDb>
{

   static
   {
      try
      {
         //bootstraps the ElasticRql type
         Class.forName(ElasticRql.class.getName());
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
      }
   }

   protected static String      url                      = null;

   protected static int         maxRequestDuration       = 10;                  // duration in seconds.

   protected static final int[] allowedFailResponseCodes = {400, 401, 403, 404};

   public ElasticDb()
   {
      super();
      withType("elastic");
   }

   @Override
   protected void startup0()
   {
      try
      {
         reflectDb();
         configApi();
      }
      catch (Exception ex)
      {
         Utils.rethrow(ex);
      }

   }

   @Override
   public Results<Row> select(Table table, List<Term> columnMappedTerms) throws Exception
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void delete(Table table, String entityKey) throws Exception
   {
      // TODO Auto-generated method stub

   }

   @Override
   public String upsert(Table table, Map<String, Object> rows) throws Exception
   {
      // TODO Auto-generated method stub
      return null;
   }

   private void reflectDb() throws Exception
   {
      if (!isBootstrap())
      {
         return;
      }

      // 'GET _all' returns all indices/aliases/mappings

      FutureResponse future = HttpUtils.get(getUrl() + "/_all");
      Response allResp = future.get(maxRequestDuration, TimeUnit.SECONDS);

      if (allResp.isSuccess())
      {
         // we now have the indices, aliases for each index, and mappings (and settings if we need them)

         JSNode jsObj = allResp.getJson();

         Map<String, JSNode> jsContentMap = jsObj.asMap();

         // a map is needed when building tables to keep track of which alias'ed indexes, such as 'all', have previously been built.
         Map<String, Table> tableMap = new HashMap<String, Table>();

         for (String key : jsContentMap.keySet())
         {
            // we now have the index and with it, it's aliases and mappings
            Object t = jsContentMap.get(key);

            buildAliasTables(key, (JSNode) t, tableMap);
         }
      }
      else
      {
         throw new ApiException(SC.matches(allResp.getStatusCode(), allowedFailResponseCodes) ? SC.SC_MAP.get(allResp.getStatus()) : SC.SC_500_INTERNAL_SERVER_ERROR);
      }

   }

   private void configApi()
   {
      for (Table t : getTables())
      {
         List<Column> cols = t.getColumns();
         Collection collection = new Collection();

         collection.withName(super.beautifyCollectionName(t.getName()));
         collection.withTable(t);

         api.withCollection(collection);
      }
   }

   /**
    * At the time of writing, there is no need to parse settings.
    * This method creates tables based on alias names of 
    * elastic indexes.  If no alias exists, no table is created.
    * 
    * The name of the elastic index will be used as a table index.
    * Most tables will only have one index. An example of a 
    * table with multiple indexes would be the alias 'all'.
    * @param indexName
    * @param jsIndex
    * @return
    */
   private void buildAliasTables(String elasticName, JSNode jsIndex, Map<String, Table> tableMap)
   {

      String aliasName = null;
      JSNode jsMappingsDocPropsNode = jsIndex.findNode("mappings._doc.properties");
      if (jsMappingsDocPropsNode != null)
      {
         Map<String, JSNode> jsMappingsDocProps = jsMappingsDocPropsNode.asMap();
         Map<String, JSNode> jsAliasProps = jsIndex.getNode("aliases").asMap();
         for (Map.Entry<String, JSNode> propEntry : jsAliasProps.entrySet())
         {
            aliasName = propEntry.getKey();

            Table table = null;

            // use the previously created table if it exists.
            if (tableMap.containsKey(aliasName))
               table = tableMap.get(aliasName);
            else
            {
               table = new Table(this, aliasName);
               tableMap.put(aliasName, table);
            }

            withTable(table);

            // use the mapping to add columns to the table.
            addColumns(table, false, jsMappingsDocProps, "");
         }
      }
   }

   /**
    * @param table - add the column to this table
    * @param nullable - lets the column nullable
    * @param jsPropsMap - contains the parent's nested properties
    * @param parentPrefix - necessary for 'nested' column names.
    */
   private void addColumns(Table table, boolean nullable, Map<String, JSNode> jsPropsMap, String parentPrefix)
   {
      for (Map.Entry<String, JSNode> propEntry : jsPropsMap.entrySet())
      {
         String colName = parentPrefix + propEntry.getKey();
         JSNode propValue = propEntry.getValue();

         // potential types include: keyword, long, nested, object, boolean
         if (propValue.hasProperty("type") && table.getColumn(colName) == null)
         {
            table.makeColumn(colName, colName);
         }
      }
   }

   public String getUrl()
   {
      return Utils.findSysEnvPropStr(getName() + ".url", url);
   }

}
