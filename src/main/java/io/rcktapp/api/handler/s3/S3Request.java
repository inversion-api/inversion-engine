package io.rcktapp.api.handler.s3;

/**
 * @author kfrankic
 *
 */
public class S3Request
{
   private String  table  = null;
   private String  key    = null;
   private String  prefix = null;
   private Integer size   = null;

   public S3Request(String table, String prefix, String key, Integer size)
   {
      this.table = table;
      this.prefix = prefix;
      this.key = key;
      this.size = size;
   }

   public String getTable()
   {
      return table;
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

}
