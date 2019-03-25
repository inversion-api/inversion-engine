package io.rocketpartners.cloud.action.s3;

/**
 * Very basic RQL for use with S3
 * 
 * @author kfrankic
 *
 */
public class S3Rql// extends Rql
{

   //   static
   //   {
   //      Rql.addRql(new S3Rql());
   //   }
   //
   //   private S3Rql()
   //   {
   //      super("s3");
   //      setDoQuote(false);
   //   }
   //
   //   // examples...
   //   // api/lift/us/s3/bucketName - retrieves all core meta
   //   // api/lift/us/s3/bucketName/inner/folder  
   //   // TODO api/lift/us/s3/bucketName?w(key,wells) 
   //
   //   // api/lift/us/s3/bucketname?download&eq(key,blerp.json) - downloads 'blerp.json' 
   //   // api/lift/us/s3/bucketname?download&sw(key,blerp) - zips & downloads all files starting with 'blerp'
   //
   //   // api/lift/us/s3/bucketName/key
   //   // api/lift/us/s3/bucketName?eq(key,filename)
   //   // TODO api/lift/us/s3/bucketName?w(key,wells)&expand - gets core & extended metadata
   //   // TODO api/lift/us/s3/bucketName?w(key,wells)&exclude(x,y) - retrieves core & extended meta, but excludes 'x & y' in the response
   //   // TODO api/lift/us/s3/bucketName?w(key,wells)&include(x,y) - retrieves core & extended meta, but only includes 'x & y' in the response
   //
   //   public S3Request buildS3Request(Request req, Table table, Integer pageSize) throws Exception
   //   {
   //      S3Request s3Req = null;
   //
   //      boolean isDownloadRequest = req.removeParam("download") != null ? true : false;
   //      String marker = req.removeParam("marker");
   //
   //      String contentType = req.getHeader("Content-Type");
   //
   //      // POST request - use the binary file name as the key.  If meta was sent with the request, 
   //      // it will be processed later when the MetaRequest is built 
   //      if (contentType != null && contentType.toLowerCase().startsWith("multipart/") && req.getUploads().size() > 0)
   //      {
   //         // TODO check for a prefix to add to the key if it exists.
   //         String prefix = determinePrefixFromPath(req.getCollectionKey(), req.getSubpath());
   //         String key = prefix == null ? req.getUploads().get(0).getFileName() : prefix + "/" + req.getUploads().get(0).getFileName();
   //         s3Req = new S3Request(table.getName(), null, key, null, false, null);
   //      }
   //
   //      if (s3Req == null)
   //      {
   //         Stmt stmt = buildStmt(new Stmt(this, null, null, table), null, req.getParams(), null);
   //         if (pageSize != null)
   //            stmt.setMaxRows(pageSize);
   //
   //         // GET request - If no queries exist manually build the s3 request
   //         if (req.getParams().size() == 1 && req.getParam("tenantid") != null)
   //         {
   //            String prefix = determinePrefixFromPath(req.getCollectionKey(), req.getSubpath());
   //            s3Req = new S3Request(stmt.table.getName(), null, prefix, stmt.pagesize, isDownloadRequest, marker);
   //         }
   //         else
   //         {
   //            s3Req = decipherStmt(stmt, isDownloadRequest, marker);
   //         }
   //      }
   //      return s3Req;
   //   }
   //
   //   private S3Request decipherStmt(Stmt stmt, boolean isDownload, String marker)
   //   {
   //      String prefix = null;
   //      String key = null;
   //
   //      for (Predicate pred : stmt.where)
   //      {
   //         switch (pred.getToken())
   //         {
   //            case "sw":
   //               switch (pred.getTerms().get(0).toString())
   //               {
   //                  case "key":
   //                     if (prefix == null)
   //                        prefix = pred.getTerms().get(1).toString();
   //                     else
   //                        throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "S3 RQL has already set a 'sw' value.");
   //               }
   //               break;
   //            case "eq":
   //               String term = pred.getTerms().get(0).toString();
   //               switch (term)
   //               {
   //                  case "key":
   //                     if (key == null)
   //                        key = pred.getTerms().get(1).toString();
   //                     else
   //                        throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "S3 RQL has already set a value on 'key'.");
   //                     break;
   //               }
   //               break;
   //            default :
   //               throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "S3 RQL does not know how to handle the function: " + pred.getToken());
   //         }
   //      }
   //
   //      return new S3Request(stmt.table.getName(), prefix, key, stmt.maxRows, isDownload, marker);
   //   }
   //
   //   private String determinePrefixFromPath(String tableName, String path)
   //   {
   //      String prefix = path.substring(tableName.length());
   //      if (prefix.startsWith("/"))
   //         prefix = prefix.substring(1);
   //      if (prefix.endsWith("/"))
   //         prefix = prefix.substring(0, prefix.length() - 1);
   //      if (prefix.equals(""))
   //         prefix = null;
   //
   //      return prefix;
   //   }

}
