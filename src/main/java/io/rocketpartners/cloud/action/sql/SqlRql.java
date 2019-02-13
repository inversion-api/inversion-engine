/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * http://rocketpartners.io
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.rocketpartners.cloud.action.sql;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.rql.Rql;

public class SqlRql extends Rql<Table, SqlQuery>
{
   public static final HashSet<String> RESERVED = new HashSet(Arrays.asList(new String[]{"as", "includes", "sort", "order", "offset", "limit", "distinct", "aggregate", "function", "sum", "count", "min", "max"}));

   static
   {
      Rql.addRql(new SqlRql("mysql"));
      Rql.addRql(new SqlRql("postgresql"));
      Rql.addRql(new SqlRql("postgres"));
      Rql.addRql(new SqlRql("redshift"));
      Rql.addRql(new SqlRql("h2"));
   }

   private SqlRql(String type)
   {
      super(type);

      if (type != null && type.toLowerCase().indexOf("mysql") > -1)
      {
         columnQuote = '`';
         //setCalcRowsFound(true);
      }

   }

   protected char stringQuote = '\'';
   protected char columnQuote = '"';

   public String quoteCol(String col)
   {
      return columnQuote + col + columnQuote;
   }

   public String quoteStr(String str)
   {
      return stringQuote + str + stringQuote;
   }

   public SqlQuery buildQuery(Object queryParams, String select)
   {
      return buildQuery(null, queryParams, select);
   }

   public SqlQuery buildQuery(Table table, Object queryParams)
   {
      return buildQuery(table, queryParams, null);
   }

   public SqlQuery buildQuery(Table table, Object queryParams, String select)
   {
      SqlQuery query = new SqlQuery(table);
      if (type != null && type.toLowerCase().indexOf("mysql") > -1)
      {
         query.withColumnQuote('`');
         //setCalcRowsFound(true);
      }
      query.withTerms(queryParams);
      query.withSelectSql(select);

      return query;
   }

}
