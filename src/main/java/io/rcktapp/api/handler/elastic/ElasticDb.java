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
            String indexName = entry.getKey();
            JSObject jsIndex = JS.toJSObject(entry.getValue());

            Table table = new Table(this, indexName);
            addTable(table);

            // use the mapping to add columns to the table.
            Map<String, String> jsMappingsDocProps = jsIndex.getObject("mappings").getObject("_doc").getObject("properties").asMap();

            for (Map.Entry<String, String> propEntry : jsMappingsDocProps.entrySet())
            {
               String colName = propEntry.getKey();


            }

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

   /**
    * This method was created specifically as a helper for creating nested columns, but ended up
    * able to be used for all types of columns.
    * @param table - add the column to this table
    * @param nullable - lets the column nullable
    * @param jsParentObj - contains the parent's nested properties
    */
   private void createColumns(Table table, boolean nullable, JSObject jsParentObj, String parentName)
   {

      for (Map.Entry<String, Object> propEntry : jsPropsMap.entrySet())
      {
         String colName = parentName + propEntry.getKey();
         JSObject propValue = (JSObject) propEntry.getValue();
         if (!propValue.getString("type").equalsIgnoreCase("nested"))
         {
            // not a 'nested' type.

            // TODO handle nested properties using the entire dot-notated name. ex: 'related.locationcode';
            // TODO what to do about column types (specifically 'keyword')
            // potential types include: keyword, long, nested, object, boolean
            String colType = null;
            Column column = new Column(table, colName, colType, true);
            table.addColumn(column);            
         }
         else
         {
            createColumns(table, true, propValue.getObject("properties"), colName + ".");

         }
      }
   }

}
