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
import io.inversion.cloud.utils.Utils;

public class CosmosSqlQuery extends SqlQuery<CosmosDocumentDb>
{
   public CosmosSqlQuery(CosmosDocumentDb db, Table table, List<Term> terms)
   {
      super(table, terms);
      super.withDb(db);
   }

   public Results<Row> doSelect() throws Exception
   {

      CosmosDocumentDb db = getDb();
      String sql = getPreparedStmt();

      String debug = "CosmosDb -> '" + sql + "' args={";

      DocumentClient cosmos = db.getDocumentClient();
      String collectionUri = db.getCollectionUri(table);

      SqlParameterCollection params = new SqlParameterCollection();

      for (int i = 0; i < values.size(); i++)
      {
         KeyValue kv = values.get(i);
         String varName = asVariableName(i);
         params.add(new SqlParameter(varName, kv.getValue()));

         debug += varName + "='" + kv.getValue() + "',";
      }
      debug += "}";

      Results results = null;

      try
      {

         SqlQuerySpec querySpec = new SqlQuerySpec(sql, params);
         FeedOptions options = new FeedOptions();
         options.setEnableCrossPartitionQuery(true);

         FeedResponse<Document> queryResults = cosmos.queryDocuments(collectionUri, querySpec, options);

         results = new Results(this);
         for (Document doc : queryResults.getQueryIterable())
         {
            String json = doc.toJson();
            JSNode node = JSNode.parseJsonNode(json);
            for (String key : node.keySet())
            {
               if(key.startsWith("_"))
                  node.remove(key);
            }
            //-- the JSON returned from cosmos looks crazy, keys are all jumbled up.
            node.sortKeys();
            results.withRow(node);
         }
      }
      finally
      {
         debug = debug.replaceAll("\r", "");
         debug = debug.replaceAll("\n", " ");
         debug = debug.trim().replaceAll(" +", " ");
         Chain.debug(debug);
      }
      return results;
   }

   protected String toSql(boolean preparedStmt)
   {
      String sql = super.toSql(preparedStmt);

      sql = sql.replace(columnQuote + table.getName() + columnQuote + ".*", "*");

      //'Orders'\.'([^']*)'
      String regex = columnQuote + table.getName() + columnQuote + "\\." + columnQuote + "([^" + columnQuote + "]*)" + columnQuote;
      sql = sql.replaceAll(regex, table.getName() + "[\"$1\"]");

      sql = sql.replace(columnQuote + table.getName() + columnQuote, table.getName());

      sql = sql.replace("LIMIT 100 OFFSET 0", "");

      return sql;
   }

   protected String asVariableName(int valuesPairIdx)
   {
      KeyValue kv = values.get(valuesPairIdx);
      return "@" + kv.getKey() + (valuesPairIdx + 1);
   }

   //   public String printTable()
   //   {
   //      return table.getName();
   //   }

   //   public String printCol(String columnName)
   //   {
   //      return table.getName() + "[\"" + columnName + "\"]";
   //   }
}
