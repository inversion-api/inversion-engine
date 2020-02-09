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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Property;
import io.inversion.cloud.model.Db;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.Results;
import io.inversion.cloud.model.Status;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.utils.HttpUtils;
import io.inversion.cloud.utils.Rows.Row;
import io.inversion.cloud.utils.Utils;

public class ElasticsearchDb extends Db<ElasticsearchDb>
{
   protected static String      url                      = null;

   protected static int         maxRequestDuration       = 10;                  // duration in seconds.

   protected static final int[] allowedFailResponseCodes = {400, 401, 403, 404};

   public ElasticsearchDb()
   {
      withType("elasticsearch");
   }

   public ElasticsearchDb(String name)
   {
      this();
      withName(name);
   }

   @Override
   public Results<Row> select(Collection table, List<Term> columnMappedTerms) throws Exception
   {
      // TODO Auto-generated method stub
      return null;
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

   private void reflectDb()
   {
      if (!isBootstrap())
      {
         return;
      }

      try
      {
         // 'GET _all' returns all indices/aliases/mappings
         String url = getUrl();
         Response allResp = HttpUtils.get(url + "/_all").get(maxRequestDuration, TimeUnit.SECONDS);

         if (allResp.isSuccess())
         {
            // we now have the indices, aliases for each index, and mappings (and settings if we need them)

            JSNode jsObj = JSNode.parseJsonNode(allResp.getContent());

            Map<String, JSNode> jsContentMap = jsObj.asMap();

            // a map is needed when building tables to keep track of which alias'ed indexes, such as 'all', have previously been built.
            Map<String, Collection> tableMap = new HashMap<String, Collection>();

            for (Map.Entry<String, JSNode> entry : jsContentMap.entrySet())
            {
               // we now have the index and with it, it's aliases and mappings
               buildAliasTables(entry.getKey(), entry.getValue(), tableMap);
            }
         }
         else
         {
            if (allResp.getError() != null)
            {
               allResp.getError().printStackTrace();
               Utils.getCause(allResp.getError()).printStackTrace();
            }
            throw new ApiException(allResp.hasStatus(allowedFailResponseCodes) ? allResp.getStatus() : Status.SC_500_INTERNAL_SERVER_ERROR);
         }
      }
      catch (Exception ex)
      {
         Utils.rethrow(ex);
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
   private void buildAliasTables(String elasticName, JSNode jsIndex, Map<String, Collection> tableMap)
   {

      String aliasName = null;
      Map<String, JSNode> jsMappingsDocProps = jsIndex.getNode("mappings").getNode("_doc").getNode("properties").asMap();
      Map<String, JSNode> jsAliasProps = jsIndex.getNode("aliases").asMap();
      for (Map.Entry<String, JSNode> propEntry : jsAliasProps.entrySet())
      {
         aliasName = propEntry.getKey();

         Collection table = null;

         // use the previously created table if it exists.
         if (tableMap.containsKey(aliasName))
            table = tableMap.get(aliasName);
         else
         {
            table = new Collection(aliasName);
            tableMap.put(aliasName, table);
         }

         withCollection(table);

         // use the mapping to add columns to the table.
         addColumns(table, false, jsMappingsDocProps, "");
      }
   }

   /**
    * @param table - add the column to this table
    * @param nullable - lets the column nullable
    * @param jsPropsMap - contains the parent's nested properties
    * @param parentPrefix - necessary for 'nested' column names.
    */
   private void addColumns(Collection table, boolean nullable, Map<String, JSNode> jsPropsMap, String parentPrefix)
   {
      int columnNumber = 0;
      for (Map.Entry<String, JSNode> propEntry : jsPropsMap.entrySet())
      {
         columnNumber += 1;

         String colName = parentPrefix + propEntry.getKey();
         JSNode propValue = propEntry.getValue();

         // potential types include: keyword, long, nested, object, boolean
         if (propValue.containsKey("type") && table.getProperty(colName) == null)
         {
            Property column = new Property(colName, propValue.getString("type"), true);
            table.withProperties(column);
         }
      }
   }

   public String getUrl()
   {
      return Utils.findSysEnvPropStr(getName() + ".url", url);
   }

   public ElasticsearchDb withUrl(String url)
   {
      this.url = url;
      return this;
   }
}