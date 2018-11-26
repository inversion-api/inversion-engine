package io.rcktapp.rql.sql;

import java.util.Arrays;
import java.util.HashSet;

import io.rcktapp.rql.Rql;

public class SqlRql extends Rql
{
   public static final HashSet<String>         RESERVED  = new HashSet(Arrays.asList(new String[]{"as", "includes", "sort", "order", "offset", "limit", "distinct", "aggregate", "function", "sum", "count", "min", "max"}));
   
   static
   {
      Rql.addRql(new SqlRql("mysql"));
      Rql.addRql(new SqlRql("postgresql"));
      Rql.addRql(new SqlRql("postgres"));
      Rql.addRql(new SqlRql("redshift"));
   }

   private SqlRql(String type)
   {
      super(type);

      if (type != null && type.toLowerCase().indexOf("mysql") > -1)
      {
         setIdentifierQuote('`');
      }
      
      for(String reserved : RESERVED)
         addReserved(reserved);
   }
}
