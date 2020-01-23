package io.inversion.cloud.action.cosmosdb;

import java.util.List;

import org.apache.commons.collections4.KeyValue;

import com.microsoft.azure.documentdb.Document;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.documentdb.FeedOptions;
import com.microsoft.azure.documentdb.FeedResponse;
import com.microsoft.azure.documentdb.SqlParameter;
import com.microsoft.azure.documentdb.SqlParameterCollection;
import com.microsoft.azure.documentdb.SqlQuerySpec;

import io.inversion.cloud.action.sql.SqlQuery;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Results;
import io.inversion.cloud.model.Table;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.utils.Rows.Row;

/**
 * @see https://docs.microsoft.com/en-us/azure/cosmos-db/sql-query-getting-started
 * @author wells
 */
public class CosmosSqlQuery extends SqlQuery<CosmosDocumentDb>
{
   public CosmosSqlQuery()
   {

   }

   public CosmosSqlQuery(CosmosDocumentDb db, Table table, List<Term> terms)
   {
      super(table, terms);
      super.withDb(db);
   }

   public Results<Row> doSelect() throws Exception
   {
      Results results = new Results(this);;
      CosmosDocumentDb db = getDb();

      String collectionUri = db.getCollectionUri(table);

      String sql = getPreparedStmt();

      SqlParameterCollection params = new SqlParameterCollection();
      for (int i = 0; i < values.size(); i++)
      {
         KeyValue kv = values.get(i);
         String varName = asVariableName(i);
         params.add(new SqlParameter(varName, kv.getValue()));
      }

      try
      {
         if (!isDryRun())
         {
            DocumentClient cosmos = db.getDocumentClient();
            SqlQuerySpec querySpec = new SqlQuerySpec(sql, params);
            FeedOptions options = new FeedOptions();
            options.setEnableCrossPartitionQuery(true);

            FeedResponse<Document> queryResults = cosmos.queryDocuments(collectionUri, querySpec, options);

            for (Document doc : queryResults.getQueryIterable())
            {
               String json = doc.toJson();
               JSNode node = JSNode.parseJsonNode(json);

               //-- removes all cosmos applied system keys that start with "_"
               //-- TODO: might want to make this a configuration option and/or
               //-- specifically blacklist known cosmos keys as this algorithm
               //-- will delete any _ prefixed property even if it was supplied
               //-- by the user
               for (String key : node.keySet())
               {
                  if (key.startsWith("_"))
                     node.remove(key);
               }
               //-- the JSON returned from cosmos looks crazy, keys are all jumbled up.
               node.sortKeys();
               results.withRow(node);
            }
         }
      }
      finally
      {
         String debug = "CosmosDb '" + sql + "' args={";

         for (int i = 0; i < values.size(); i++)
         {
            debug += asVariableName(i) + "='" + values.get(i).getValue();
            if (i < values.size() - 1)
               debug += ",";
         }
         debug += "}";
         debug = debug.replaceAll("\r", "");
         debug = debug.replaceAll("\n", " ");
         debug = debug.trim().replaceAll(" +", " ");
         Chain.debug(debug);

         results.withTestQuery(debug);

         debug = getDynamicStmt();
         debug = debug.replaceAll("\r", "");
         debug = debug.replaceAll("\n", " ");
         debug = debug.trim().replaceAll(" +", " ");

         //TODO: how do we efficiently skip this
         results.withDebugQuery(debug);

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
   protected String toSql(boolean preparedStmt)
   {
      String sql = super.toSql(preparedStmt);

      sql = sql.replace(columnQuote + table.getName() + columnQuote + ".*", "*");

      String regex = columnQuote + table.getName() + columnQuote + "\\." + columnQuote + "([^" + columnQuote + "]*)" + columnQuote;
      sql = sql.replaceAll(regex, table.getName() + "[\"$1\"]");

      sql = sql.replace(columnQuote + table.getName() + columnQuote, table.getName());

      return sql;
   }

   /**
    * Both offset and limit are required per cosmos spec.
    * 
    * @see https://docs.microsoft.com/en-us/azure/cosmos-db/sql-query-offset-limit
    */
   protected String printLimitClause(Parts parts, int offset, int limit)
   {
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
   protected String asVariableName(int valuesPairIdx)
   {
      KeyValue kv = values.get(valuesPairIdx);
      return "@" + kv.getKey() + (valuesPairIdx + 1);
   }

}
