package io.rcktapp.rql.s3;

import java.util.Map;

import io.rcktapp.api.ApiException;
import io.rcktapp.api.SC;
import io.rcktapp.api.Table;
import io.rcktapp.api.handler.s3.S3Request;
import io.rcktapp.rql.Predicate;
import io.rcktapp.rql.Rql;
import io.rcktapp.rql.Stmt;

/**
 * Very basic RQL for use with S3
 * 
 * @author kfrankic
 *
 */
public class S3Rql extends Rql
{

   static
   {
      Rql.addRql(new S3Rql());
   }

   private S3Rql()
   {
      super("s3");
      setDoQuote(false);
   }

   // examples...
   // api/lift/us/s3/bucketName - retrieves all core meta
   // api/lift/us/s3/bucketName?w(key,wells) - retrieves core meta

   // api/lift/us/s3/bucketname?download&eq(key,blerp.json) - downloads 'blerp.json' 
   // api/lift/us/s3/bucketname?download&sw(key,blerp) - zips & downloads all files starting with 'blerp'

   // api/lift/us/s3/bucketName?w(key,wells)&expand - gets core & extended metadata
   // api/lift/us/s3/bucketName?w(key,wells)&exclude(x,y) - retrieves core & extended meta, but excludes 'x & y' in the response
   // api/lift/us/s3/bucketName?w(key,wells)&include(x,y) - retrieves core & extended meta, but only includes 'x & y' in the response

   public S3Request buildS3Request(Map<String, String> requestParams, Table table, Integer pageSize) throws Exception
   {
      Stmt stmt = buildStmt(new Stmt(this, null, null, table), null, requestParams, null);
      stmt.setMaxRows(pageSize);
      return decipherStmt(stmt);
   }

   private S3Request decipherStmt(Stmt stmt)
   {
      String prefix = null;
      String key = null;

      for (Predicate pred : stmt.where)
      {
         switch (pred.getToken())
         {
            case "sw":
               switch (pred.getTerms().get(0).toString())
               {
                  case "key":
                     if (prefix == null)
                        prefix = pred.getTerms().get(1).toString();
                     else
                        throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "S3 RQL has already set a 'sw' value.");
               }
               break;
            case "eq":
               String term = pred.getTerms().get(0).toString();
               switch (term)
               {
                  case "key":
                     if (key == null)
                        key = pred.getTerms().get(1).toString();
                     else
                        throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "S3 RQL has already set a value on 'key'.");
                     break;
               }
               break;
            default :
               throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "S3 RQL does not know how to handle the function: " + pred.getToken());
         }
      }

      return new S3Request(stmt.table.getName(), prefix, key, stmt.maxRows);
   }

}
