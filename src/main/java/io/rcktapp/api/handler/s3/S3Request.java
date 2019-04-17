package io.rcktapp.api.handler.s3;

/**
 * @author kfrankic
 *
 */
public class S3Request
{
   private String  bucket  = null;
   private String  key    = null;
   private String  prefix = null;
   private Integer size   = null;
   
   private String  marker   = null;
   private boolean download = false;
   private boolean meta = false;

   public S3Request(String bucket, String prefix, String key, Integer size, boolean download, boolean meta, String marker)
   {
      this.bucket = bucket;
      this.prefix = prefix;
      this.key = key;
      this.size = size;
      this.download = download;
      this.meta = meta;
      this.marker = marker;
   }

   public String getBucket()
   {
      return bucket;
   }

   public String getKey()
   {
      return key;
   }

   public String getPrefix()
   {
      return prefix;
   }

   public Integer getSize()
   {
      return size;
   }

   public boolean isDownload()
   {
      return download;
   }

   public boolean isMeta()
   {
      return meta;
   }

   public String getMarker()
   {
      return marker;
   }

}
