package io.rcktapp.rql.sql;

import io.rcktapp.rql.Rql;

public class SqlRql extends Rql
{
   static
   {
      Rql.addRql(new SqlRql("mysql"));
      Rql.addRql(new SqlRql("postgresql"));
      Rql.addRql(new SqlRql("redshift"));
   }

   private SqlRql(String type)
   {
      super(type);

      if (type != null && type.toLowerCase().indexOf("mysql") > -1)
      {
         setIdentifierQuote('`');
      }
   }
}
