/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * http://rocketpartners.io
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.rocketpartners.cloud.action.elastic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Attribute;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Column;
import io.rocketpartners.cloud.model.Db;
import io.rocketpartners.cloud.model.Entity;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.utils.JS;
import io.rocketpartners.cloud.utils.JSObject;
import io.rocketpartners.cloud.utils.Web;

public class ElasticsearchDb extends Db
{
   protected static String      url                      = null;

   protected static int         maxRequestDuration       = 10;                  // duration in seconds.

   protected static final int[] allowedFailResponseCodes = {400, 401, 403, 404};

   @Override
   public void bootstrapApi() throws Exception
   {
      this.setType("elastic");

      reflectDb();
      configApi();
   }

   private void reflectDb() throws Exception
   {
      if (!isBootstrap())
      {
         return;
      }

      // 'GET _all' returns all indices/aliases/mappings
      Web.Response allResp = Web.get(url + "/_all", 0).get(maxRequestDuration, TimeUnit.SECONDS);

      if (allResp.isSuccess())
      {
         // we now have the indices, aliases for each index, and mappings (and settings if we need them)

         JSObject jsObj = JS.toJSObject(allResp.getContent());

         Map<String, JSObject> jsContentMap = jsObj.asMap();

         // a map is needed when building tables to keep track of which alias'ed indexes, such as 'all', have previously been built.
         Map<String, Table> tableMap = new HashMap<String, Table>();

         for (Map.Entry<String, JSObject> entry : jsContentMap.entrySet())
         {
            // we now have the index and with it, it's aliases and mappings
            buildAliasTables(entry.getKey(), entry.getValue(), tableMap);
         }
      }
      else
      {
         throw new ApiException(SC.matches(allResp.getCode(), allowedFailResponseCodes) ? SC.SC_MAP.get(allResp.getCode()) : SC.SC_500_INTERNAL_SERVER_ERROR);
      }

   }

   private void configApi()
   {
      for (Table t : getTables())
      {
         List<Column> cols = t.getColumns();
         Collection collection = new Collection();

         collection.setName(lowercaseAndPluralizeString(t.getName()));

         Entity entity = new Entity();
         entity.withTable(t);
         entity.setHint(t.getName());
         entity.withCollection(collection);

         collection.setEntity(entity);

         for (Column col : cols)
         {
            Attribute attr = new Attribute();
            attr.setEntity(entity);
            attr.setName(col.getName());
            attr.setColumn(col);
            attr.setHint(col.getTable().getName() + "." + col.getName());
            attr.setType(col.getType());

            entity.withAttribute(attr);
         }

         api.withCollection(collection);
         collection.setApi(api);
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
   private void buildAliasTables(String elasticName, JSObject jsIndex, Map<String, Table> tableMap)
   {

      String aliasName = null;
      Map<String, JSObject> jsMappingsDocProps = jsIndex.getObject("mappings").getObject("_doc").getObject("properties").asMap();
      Map<String, JSObject> jsAliasProps = jsIndex.getObject("aliases").asMap();
      for (Map.Entry<String, JSObject> propEntry : jsAliasProps.entrySet())
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

         addTable(table);

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
   private void addColumns(Table table, boolean nullable, Map<String, JSObject> jsPropsMap, String parentPrefix)
   {
      int columnNumber = 0;
      for (Map.Entry<String, JSObject> propEntry : jsPropsMap.entrySet())
      {
         columnNumber += 1;

         String colName = parentPrefix + propEntry.getKey();
         JSObject propValue = propEntry.getValue();

         // potential types include: keyword, long, nested, object, boolean
         Column column = null;
         if (propValue.hasProperty("type"))
         {
            column = new Column(table, columnNumber, colName, propValue.getString("type"), true);
            table.addColumn(column);
         }
      }
   }

   public static String getURL()
   {
      return url;
   }

}
