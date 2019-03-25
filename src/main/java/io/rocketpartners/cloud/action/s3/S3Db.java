package io.rocketpartners.cloud.action.s3;

/**
 * Bucket ~= Table
 * Bucket Object field key value ~= Column
 * Only mapping the key field since it is the only way to query anything within S3,
 * since, as of now, you can't request files by size, or content-type, or some 
 * custom header.
 * 
 * @author kfrankic
 *
 */
public class S3Db// extends Db
{
//   static
//   {
//      try
//      {
//         //bootstraps the S3Rql type
//         Class.forName(S3Rql.class.getName());
//      }
//      catch (Exception ex)
//      {
//         ex.printStackTrace();
//      }
//   }
//
//   protected String accessKey = null;
//   protected String secretKey = null;
//   protected String awsRegion = null;
//
//   protected String bucket    = null;
//
//   private AmazonS3 client    = null;
//
//   /**
//    * @see io.rcktapp.api.Db#bootstrapApi()
//    */
//   @Override
//   public void bootstrapApi() throws Exception
//   {
//      client = getS3Client();
//
//      setType("s3");
//
//      // get all of the buckets this account has access to.  
//      List<Bucket> bucketList = client.listBuckets();
//
//      for (Bucket bucket : bucketList)
//      {
//         Table table = new Table(this, bucket.getName());
//
//         // Hardcoding 'key' as the only column as there is no useful way to use the other metadata
//         // for querying 
//         // Other core metadata includes: eTag, size, lastModified, storageClass
//         table.addColumn(new Column(table, "key", "java.lang.String", false));
//         addTable(table);
//      }
//
//      configApi();
//
//      client.shutdown();
//
//   }
//
//   private void configApi()
//   {
//      for (Table t : getTables())
//      {
//         List<Column> cols = t.getColumns();
//         Collection collection = new Collection();
//
//         collection.setName(lowercaseAndPluralizeString(t.getName()));
//
//         Entity entity = new Entity();
//         entity.setTbl(t);
//         entity.setHint(t.getName());
//         entity.setCollection(collection);
//
//         collection.setEntity(entity);
//
//         for (Column col : cols)
//         {
//            Attribute attr = new Attribute();
//            attr.setEntity(entity);
//            attr.setName(col.getName());
//            attr.setColumn(col);
//            attr.setHint(col.getTable().getName() + "." + col.getName());
//            attr.setType(col.getType());
//
//            entity.addAttribute(attr);
//         }
//
//         api.addCollection(collection);
//         collection.setApi(api);
//      }
//   }
//
//   private AmazonS3 getS3Client()
//   {
//      if (client != null)
//         return client;
//
//      AmazonS3ClientBuilder builder = null;
//      if (accessKey != null)
//      {
//         BasicAWSCredentials creds = new BasicAWSCredentials(accessKey, secretKey);
//         builder = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(creds));
//      }
//      else
//      {
//         builder = AmazonS3ClientBuilder.standard();
//      }
//
//      if (awsRegion != null)
//      {
//         builder.withRegion(awsRegion);
//      }
//      return builder.build();
//   }
//
//   public S3Object getDownload(S3Request req)
//   {
//      client = getS3Client();
//      return client.getObject(new GetObjectRequest(req.getBucket(), req.getKey()));
//   }
//
//   public ObjectMetadata getExtendedMetaData(S3Request req)
//   {
//      String key = req.getKey();
//      String prefix = req.getPrefix();
//
//      client = getS3Client();
//      return client.getObjectMetadata(new GetObjectMetadataRequest(req.getBucket(), prefix != null ? prefix + key : key));
//   }
//
//   public PutObjectResult saveFile(InputStream inputStream, String bucketName, String key, ObjectMetadata meta)
//   {
//      client = getS3Client();
//      return client.putObject(new PutObjectRequest(bucketName, key, inputStream, meta));
//   }
//
//   /**
//    * 
//    * @param bucketName
//    * @param size - number of files returned in the listing
//    * @param startFile - the starting point in which the list begins after.
//    * @return
//    */
//   public ObjectListing getCoreMetaData(S3Request s3Req)
//   {
//      String prefix = s3Req.getPrefix();
//      String key = s3Req.getKey();
//
//      if (prefix != null)
//      {
//         if (key != null)
//            key = prefix + key;
//         else
//            key = prefix;
//      }
//
//      client = getS3Client();
//
//      ListObjectsRequest req = new ListObjectsRequest();
//      req.setBucketName(s3Req.getBucket());
//      req.setMaxKeys(s3Req.getSize()); // TODO fix pagesize...currently always set to 1000 ... tied to 'size' but not 'pagesize'?
//      req.setDelimiter("/");
//      req.setMarker(s3Req.getMarker());
//      req.setPrefix(prefix);
//
//      return client.listObjects(req);
//   }
//
//   public CopyObjectResult updateObject(String bucket, String key, String newBucket, String newKey, ObjectMetadata meta)
//   {
//      client = getS3Client();
//      
//      CopyObjectRequest copyReq = null;
//
//      if (meta != null)
//      {
//         copyReq = new CopyObjectRequest(bucket, key, newBucket, newKey).withNewObjectMetadata(meta);
//      }
//      else
//      {
//         // rename or move request
//         copyReq = new CopyObjectRequest(bucket, key, newBucket, newKey);
//      }
//
//      // TODO if the key and newKey are not equal, (or the bucket and newBucket) delete the old key file
//      return client.copyObject(copyReq);
//   }

}
