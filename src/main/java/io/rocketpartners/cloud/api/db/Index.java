package io.rocketpartners.cloud.api.db;

public class Index extends io.rocketpartners.db.Index<Table, Column>
{
   public Index()
   {
      super();
   }

   public Index(Table table, String name, String type)
   {
      super();
      this.table = table;
      this.name = name;
      this.type = type;
   }
}
