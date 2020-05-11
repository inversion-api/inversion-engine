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
package io.inversion.elasticsearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.inversion.ApiException;
import io.inversion.Chain;
import io.inversion.Collection;
import io.inversion.Db;
import io.inversion.Property;
import io.inversion.Results;
import io.inversion.rql.Term;
import io.inversion.utils.JSNode;
import io.inversion.utils.Rows.Row;
import io.inversion.utils.Utils;

public class ElasticsearchDb extends Db<ElasticsearchDb>
{
   protected static String               url                      = null;

   protected static int                  maxRequestDuration       = 10000;               // duration in milliseconds.

   protected static final int[]          allowedFailResponseCodes = {400, 401, 403, 404};

   // This is the expected maximum base query search size.  Searching beyond this value requires
   // a 'search_after' to be performed.  By default, Elastic sets this value to 10k. Typically,
   // there's no need to change this value.
   public static int                     maxElasticQuerySize      = 10000;

   // When an elastic search is performed, this is the default '_source' value that will be used.
   // This value is optional and does not need to be set.
   public String                         defaultSource            = null;

   transient private RestHighLevelClient client;

   public ElasticsearchDb()
   {
      withType("elasticsearch");
   }

   public ElasticsearchDb(String name)
   {
      this();
      withName(name);
   }

   public RestHighLevelClient getElasticClient()
   {
      if (this.client == null)
      {
         synchronized (this)
         {
            if (this.client == null)
            {
               // TODO use this.url
               this.client = buildElasticClient("http", "localhost");
            }
         }
      }

      return client;
   }

   public static RestHighLevelClient buildElasticClient(String scheme, String hostPrefix)
   {
      RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(//
            new HttpHost(hostPrefix, 9200, scheme), //
            new HttpHost(hostPrefix, 9201, scheme)).setMaxRetryTimeoutMillis(ElasticsearchDb.maxRequestDuration));

      // TODO ApiException.throw500InternalServerError(error) if the client does not have the proper settings

      return client;
   }

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

      JSNode json = query.getJson();
      SearchResponse res = null;
      if (json == null)
      {
         SearchRequest searchReq = new SearchRequest(table.getTableName());
         searchReq.source(query.getSearchBuilder());
         res = getElasticClient().search(searchReq, RequestOptions.DEFAULT);
      }

      Results result = new Results(query);

      int statusCode = res.status().getStatus();
      if (isSuccess(statusCode))
      {
         //         result.withRow(null);
         //         result.withRows(null);
         //         result.withNext(next)

         SearchHits hits = res.getHits();
         System.out.println(hits.toString()); // TODO verify toString() output is pretty

         Long totalHits = hits.getTotalHits();

         SearchHit[] hitArray = hits.getHits();

         for (SearchHit hit : hitArray)
         {
            result.withRow(hit.getSourceAsMap()); // TODO verify getSourceAsMap() 
         }

         result.withFoundRows(totalHits.intValue());
      }
      else
         System.out.println("request failed :*(");

      return result;
   }

   @Override
   public void delete(Collection table, List<Map<String, Object>> indexValues) throws Exception
   {
      for (Map<String, Object> row : indexValues)
      {
         deleteRow(table, row);
      }
   }

   /**
    * Deletes a single specific resource.
    * 
    * @param table
    * @param indexValues
    * @throws Exception
    */
   protected void deleteRow(Collection table, Map<String, Object> indexValues) throws Exception
   {
      Object id = table.encodeKey(indexValues);

      try
      {
         DeleteRequest request = new DeleteRequest(table.getTableName(), "doc", id.toString());

         Chain.debug("ElasticDb: Delete request=" + request.toString());

         DeleteResponse response = getElasticClient().delete(request, RequestOptions.DEFAULT);

         int statusCode = response.status().getStatus();
         if (statusCode >= 400)
         {
            ApiException.throw500InternalServerError("Unexpected http status code returned from database: %s", statusCode);
         }
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
         ApiException.throw500InternalServerError(ex);

      }
   }

   @Override
   public List upsert(Collection table, List<Map<String, Object>> rows) throws Exception
   {
      List keys = new ArrayList();
      for (Map<String, Object> row : rows)
      {
         keys.add(upsertRow(table, row));
      }
      return keys;
   }

   public String upsertRow(Collection table, Map<String, Object> columnMappedTermsRow) throws Exception
   {
      JSNode doc = new JSNode(columnMappedTermsRow);

      String id = doc.getString("id");
      if (id == null)
      {
         id = table.encodeKey(columnMappedTermsRow);
         if (id == null)
            ApiException.throw400BadRequest("Your record does not contain the required key fields.");
         doc.putFirst("id", id);
      }

      String json = doc.toString();

      UpdateRequest updateRequest = new UpdateRequest(table.getTableName(), "doc", id.toString());
      updateRequest.upsert(json, XContentType.JSON);

      Chain.debug("ElasticDb: Upsert " + updateRequest.toString());

      UpdateResponse response = getElasticClient().update(updateRequest, RequestOptions.DEFAULT);

      int statusCode = response.status().getStatus();
      if (statusCode > 299)
      {
         ApiException.throw400BadRequest("Unexpected http status code returned from database: '%s'", statusCode);
      }

      String returnedId = response.getId();
      if (!Utils.equal(id, returnedId))
         ApiException.throw500InternalServerError("The supplied 'id' field does not match the returned 'id' field: '%s' vs. '%s'", id, returnedId);

      return id;
   }

   private void handleAutoSuggestRequest()
   {
      // TODO
   }

   private void handleSearchRequest()
   {
      // TODO
   }

   private void handlePaging(WrappedQueryBuilder builder)
   {
      // TODO
   }

   public void configDb() throws Exception
   {

      try
      {
         // 'GET _all' returns all indices/aliases/mappings

         // There are better ways to get this data in new versions of elastic =/
         //               ClusterHealthResponse response = getElasticClient().cluster().health(new ClusterHealthRequest(), RequestOptions.DEFAULT);
         Response allResponse = getElasticClient().getLowLevelClient().performRequest(new Request("GET", "_all"));

         int statusCode = allResponse.getStatusLine().getStatusCode();
         if (isSuccess(statusCode))
         {
            // we now have the indices, aliases for each index, and mappings (and settings if we need them)

            JSNode jsObj = JSNode.parseJsonNode(EntityUtils.toString(allResponse.getEntity()));

            Map<String, JSNode> jsContentMap = jsObj.asMap();

            // a map is needed when building tables to keep track of which alias'ed indexes, such as 'all', have previously been built.
            Map<String, Collection> tableMap = new HashMap<String, Collection>();

            for (Map.Entry<String, JSNode> entry : jsContentMap.entrySet())
            {
               // we now have the index and with it, it's aliases and mappings
               buildTables(entry.getKey(), entry.getValue(), tableMap);
            }
         }
         else
         {
            //                  if (allResp.getError() != null)
            //                  {
            //                     allResp.getError().printStackTrace();
            //                     Utils.getCause(allResp.getError()).printStackTrace();
            //                  }
            throw new ApiException("Failed to retieve /_all with status code:" + statusCode);

         }
      }
      catch (Exception ex)
      {
         Utils.rethrow(ex);
      }

   }

   //   public void configApi(Api api)
   //   {
   //      for (Collection t : getCollections())
   //      {
   //         //            List<Column> cols = t.getColumns();
   //         List<Property> columnPropList = t.getProperties();
   //         Collection collection = new Collection();
   //
   //         collection.withName(super.beautifyCollectionName(t.getName()));
   //
   //         // TODO unsure if the following still needs to occur...
   //
   //         //collection.withTable(t); // Entity entity = collection.withEntity(t);
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

   /**
    * At the time of writing, there is no need to parse settings.
    * This method creates tables based on alias names of 
    * elastic indexes.  A table of the index will always be created
    * at a minimum.
    * 
    * Most tables will only have one index. An example of a 
    * table with multiple indexes would be the alias 'all'.
    * @param indexName
    * @param jsIndex
    * @return
    */
   private void buildTables(String elasticName, JSNode jsIndex, Map<String, Collection> tableMap)
   {
      Map<String, JSNode> jsMappingsDocProps = jsIndex.getNode("mappings").getNode("_doc").getNode("properties").asMap();

      Collection table = null;

      if (tableMap.containsKey(elasticName))
         table = tableMap.get(elasticName);
      else
      {
         table = new Collection(elasticName).withDb(this);
         tableMap.put(elasticName, table);
      }

      withCollection(table);

      // use the mapping to add columns to the table.
      addColumns(table, false, jsMappingsDocProps, "");

      String aliasName = null;
      Map<String, ObjectNode> jsAliasProps = jsIndex.getNode("aliases").asMap();
      for (Map.Entry<String, ObjectNode> propEntry : jsAliasProps.entrySet())
      {
         aliasName = propEntry.getKey();

         table = null;

         // use the previously created table if it exists.
         if (tableMap.containsKey(aliasName))
            table = tableMap.get(aliasName);
         else
         {
            table = new Collection(aliasName).withDb(this);
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
      //      int columnNumber = 0;
      for (Map.Entry<String, JSNode> propEntry : jsPropsMap.entrySet())
      {
         //         columnNumber += 1;

         String colName = parentPrefix + propEntry.getKey();
         JSNode propValue = propEntry.getValue();

         // potential types include: keyword, long, nested, object, boolean
         if (propValue.containsKey("type") && table.getProperty(colName) == null)
         {
            //               Column column = new Column(table, columnNumber, colName, propValue.getString("type"), true);
            Property columnProp = new Property(colName, propValue.getString("type"), true);
            table.withProperties(columnProp);
         }
      }
   }

   public String getUrl()
   {
      return Utils.getSysEnvPropStr(getName() + ".url", url);
   }

   public ElasticsearchDb withUrl(String url)
   {
      this.url = url;
      return this;
   }

   private boolean isSuccess(int statusCode)
   {
      return (statusCode >= 200 && statusCode <= 300);
   }
}