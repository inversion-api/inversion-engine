package io.rcktapp.api.handler.elastic;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.forty11.web.Web;
import io.forty11.web.js.JS;
import io.forty11.web.js.JSObject;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Column;
import io.rcktapp.api.Db;
import io.rcktapp.api.SC;
import io.rcktapp.api.Table;

public class ElasticDb extends Db
{

   // TODO elasticURL and SC_MAP are duplicated variables from ElasticHandler...they should not be housed in two separate locations.

   String                      elasticURL = "https://vpc-liftck-gen2-dev-f44d6n5phip7ffw3js6lqa4hda.us-east-1.es.amazonaws.com";

   static Map<Integer, String> SC_MAP     = new HashMap<>();
   static
   {
      SC_MAP.put(400, SC.SC_400_BAD_REQUEST);
      SC_MAP.put(401, SC.SC_401_UNAUTHORIZED);
      SC_MAP.put(403, SC.SC_403_FORBIDDEN);
      SC_MAP.put(404, SC.SC_404_NOT_FOUND);
   }

   @Override
   public void bootstrapApi(Api api) throws Exception
   {
      reflectDb();
      configApi(api);
   }

   private void reflectDb() throws Exception
   {
      if (!isBootstrap())
      {
         return;
      }

      // 'GET _all' returns all indices/aliases/mappings
      Web.Response allResp = Web.get(elasticURL + "/_all", 0).get(10, TimeUnit.SECONDS);

      if (allResp.isSuccess())
      {
         // we now have the indices, aliases for each index, and mappings (and settings if we need them)

         JSObject jsObj = JS.toJSObject(allResp.getContent());

         Map<String, String> jsContentMap = jsObj.asMap();

         for (Map.Entry<String, String> entry : jsContentMap.entrySet())
         {
            // we now have the index and with it, it's aliases and mappings
            createTable(entry.getKey(), JS.toJSObject(entry.getValue()));
         }
      }
      else
      {
         String status = SC_MAP.get(allResp.getCode());
         throw new ApiException(status != null ? status : SC.SC_500_INTERNAL_SERVER_ERROR);
      }

   }

   private void configApi(Api api)
   {

   }
   
   private void createTable(String indexName, JSObject jsIndex)
   {
      Table table = new Table(this, indexName);
      addTable(table);

      // use the mapping to add columns to the table.
      Map<String, Object> jsMappingsDocProps = jsIndex.getObject("mappings").getObject("_doc").getObject("properties").asMap();
      addColumns(table, false, jsMappingsDocProps, "");
   }

   /**
    * @param table - add the column to this table
    * @param nullable - lets the column nullable
    * @param jsPropsMap - contains the parent's nested properties
    * @param parentPrefix - necessary for 'nested' column names.
    */
   private void addColumns(Table table, boolean nullable, Map<String, Object> jsPropsMap, String parentPrefix)
   {
      for (Map.Entry<String, Object> propEntry : jsPropsMap.entrySet())
      {
         String colName = parentPrefix + propEntry.getKey();
         JSObject propValue = (JSObject) propEntry.getValue();
         if (!propValue.getString("type").equalsIgnoreCase("nested"))
         {
            // not a 'nested' type.

            // TODO what to do about column types (specifically 'keyword')
            // potential types include: keyword, long, nested, object, boolean
            String colType = null;
            Column column = new Column(table, colName, colType, true);
            table.addColumn(column);            
         }
         else
         {
            addColumns(table, true, propValue.getObject("properties").asMap(), colName + ".");

         }
      }
   }

}
