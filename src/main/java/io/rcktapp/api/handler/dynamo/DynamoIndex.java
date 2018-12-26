package io.rcktapp.api.handler.dynamo;

import io.rcktapp.api.Index;
import io.rcktapp.api.Table;

/**
 * Used to keep track of Partition and Sort keys for a dynamo index.
 * 
 * @author kfrankic
 *
 */
public class DynamoIndex extends Index
{
   protected String partitionKey = null;
   protected String sortKey = null;

   public DynamoIndex()
   {
      super();
   }
   
   public DynamoIndex(Table table, String name, String type)
   {
      super(table, name, type);
   }

   public DynamoIndex(Table table, String name, String type, String pk, String sk)
   {
      super(table, name, type);
      this.partitionKey = pk;
      this.sortKey = sk;
   }

   public String getPartitionKey()
   {
      return partitionKey;
   }

   public void setPartitionKey(String partitionKey)
   {
      this.partitionKey = partitionKey;
   }

   public String getSortKey()
   {
      return sortKey;
   }

   public void setSortKey(String sortKey)
   {
      this.sortKey = sortKey;
   }
   
}
