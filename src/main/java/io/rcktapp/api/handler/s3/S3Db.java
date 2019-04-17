package io.rcktapp.api.handler.s3;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;

import io.rcktapp.api.Attribute;
import io.rcktapp.api.Collection;
import io.rcktapp.api.Column;
import io.rcktapp.api.Db;
import io.rcktapp.api.Entity;
import io.rcktapp.api.Table;
import io.rcktapp.rql.s3.S3Rql;

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
public class S3Db extends Db
{
   static
   {
      try
      {
         //bootstraps the S3Rql type
         Class.forName(S3Rql.class.getName());
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
      }
   }

   protected String  accessKey       = null;
   protected String  secretKey       = null;
   protected String  awsRegion       = null;

   // If you want to limit which buckets are included, set this to a csv of bucket names. 
   // Leave this null if you want to configure all buckets in the account.
   protected String  buckets         = null;

   // Set this to true if you would like the default behavior to download the file instead of returning the files meta data
   protected boolean defaultDownload = false;

   private AmazonS3  client          = null;

   /**
    * @see io.rcktapp.api.Db#bootstrapApi()
    */
   @Override
   public void bootstrapApi() throws Exception
   {
      client = getS3Client();

      setType("s3");

      List<String> bucketNames = new ArrayList<>();
      if (buckets != null)
      {
         // use configured buckets only
         bucketNames.addAll(Arrays.asList(buckets.split(",")));
      }
      else
      {
         // get all of the buckets this account has access to.  
         List<Bucket> bucketList = client.listBuckets();
         for (Bucket bucket : bucketList)
         {
            bucketNames.add(bucket.getName());
         }
      }

      for (String bucketName : bucketNames)
      {
         Table table = new Table(this, bucketName.trim());

         // Hardcoding 'key' as the only column as there is no useful way to use the other metadata
         // for querying 
         // Other core metadata includes: eTag, size, lastModified, storageClass
         table.addColumn(new Column(table, "key", "java.lang.String", false));
         addTable(table);
      }

      configApi();

      client.shutdown();

   }

   private void configApi()
   {
      for (Table t : getTables())
      {
         List<Column> cols = t.getColumns();
         Collection collection = new Collection();

         collection.setName(lowercaseAndPluralizeString(t.getName()));

         Entity entity = new Entity();
         entity.setTbl(t);
         entity.setHint(t.getName());
         entity.setCollection(collection);

         collection.setEntity(entity);

         for (Column col : cols)
         {
            Attribute attr = new Attribute();
            attr.setEntity(entity);
            attr.setName(col.getName());
            attr.setColumn(col);
            attr.setHint(col.getTable().getName() + "." + col.getName());
            attr.setType(col.getType());

            entity.addAttribute(attr);
         }

         api.addCollection(collection);
         collection.setApi(api);
      }
   }

   private AmazonS3 getS3Client()
   {
      if (client != null)
         return client;

      AmazonS3ClientBuilder builder = null;
      if (accessKey != null)
      {
         BasicAWSCredentials creds = new BasicAWSCredentials(accessKey, secretKey);
         builder = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(creds));
      }
      else
      {
         builder = AmazonS3ClientBuilder.standard();
      }

      if (awsRegion != null)
      {
         builder.withRegion(awsRegion);
      }
      return builder.build();
   }

   public S3Object getDownload(S3Request req)
   {
      client = getS3Client();
      return client.getObject(new GetObjectRequest(req.getBucket(), req.getKey()));
   }

   public ObjectMetadata getExtendedMetaData(S3Request req)
   {
      String key = req.getKey();
      String prefix = req.getPrefix();

      client = getS3Client();
      return client.getObjectMetadata(new GetObjectMetadataRequest(req.getBucket(), prefix != null ? prefix + key : key));
   }

   public PutObjectResult saveFile(InputStream inputStream, String bucketName, String key, ObjectMetadata meta)
   {
      client = getS3Client();
      return client.putObject(new PutObjectRequest(bucketName, key, inputStream, meta));
   }

   /**
    * 
    * @param bucketName
    * @param size - number of files returned in the listing
    * @param startFile - the starting point in which the list begins after.
    * @return
    */
   public ObjectListing getCoreMetaData(S3Request s3Req)
   {
      String prefix = s3Req.getPrefix();
      String key = s3Req.getKey();

      if (prefix != null)
      {
         if (key != null)
            key = prefix + key;
         else
            key = prefix;
      }

      client = getS3Client();

      ListObjectsRequest req = new ListObjectsRequest();
      req.setBucketName(s3Req.getBucket());
      req.setMaxKeys(s3Req.getSize()); // TODO fix pagesize...currently always set to 1000 ... tied to 'size' but not 'pagesize'?
      req.setDelimiter("/");
      req.setMarker(s3Req.getMarker());
      req.setPrefix(prefix);

      return client.listObjects(req);
   }

   public CopyObjectResult updateObject(String bucket, String key, String newBucket, String newKey, ObjectMetadata meta)
   {
      client = getS3Client();

      CopyObjectRequest copyReq = null;

      if (meta != null)
      {
         copyReq = new CopyObjectRequest(bucket, key, newBucket, newKey).withNewObjectMetadata(meta);
      }
      else
      {
         // rename or move request
         copyReq = new CopyObjectRequest(bucket, key, newBucket, newKey);
      }

      // TODO if the key and newKey are not equal, (or the bucket and newBucket) delete the old key file
      return client.copyObject(copyReq);
   }

   public String getBuckets()
   {
      return buckets;
   }

   public void setBuckets(String buckets)
   {
      this.buckets = buckets;
   }

   public boolean isDefaultDownload()
   {
      return defaultDownload;
   }

   public void setDefaultDownload(boolean defaultDownload)
   {
      this.defaultDownload = defaultDownload;
   }

}
