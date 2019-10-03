/*
 * Copyright (c) 2015-2018 Inversion.org, LLC
 * https://github.com/inversion-api
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.inversion.cloud.action.elastic.v03x.dsl;

/**
 * @author kfrankic
 *
 */
public class NestedQuery extends ElasticQuery
{

   private String path;
   private QueryDsl query;
   
   /**
    * @return the path
    */
   public String getPath()
   {
      return path;
   }
   
   /**
    * @param path the path to set
    */
   public void setPath(String path)
   {
      this.path = path;
   }
   
   /**
    * @return the query
    */
   public QueryDsl getQuery()
   {
      return query;
   }
   
   /**
    * @param query the query to set
    */
   public void setQuery(QueryDsl query)
   {
      this.query = query;
   }
   
   
}
