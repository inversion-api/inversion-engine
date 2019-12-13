/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
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
package io.inversion.cloud.action.elastic;

import io.inversion.cloud.rql.Page;

public class ElasticsearchPage<T extends ElasticsearchPage, P extends ElasticsearchQuery> extends Page<T, P>
{
   public ElasticsearchPage(P query)
   {
      super(query);
      withFunctions("start");
   }

   //   // A 'start' param indicates an elastic 'search after' query should be used.
   //   // 'search after' queries should ONLY be used if it is believe the result 
   //   // will come from a row index > 10k.  
   //   if (params.containsKey("start"))
   //   {
   //      List<String> searchAfterList = Arrays.asList(params.remove("start").split(","));
   //      if (pageNum * size > MAX_NORMAL_ELASTIC_QUERY_SIZE - 1)
   //      {
   //         for (int i = 0; i < searchAfterList.size(); i++)
   //         {
   //            if (searchAfterList.get(i).equals("[NULL]")) // [NULL] is used to indicate an actual null value, not a null string.
   //               searchAfterList.set(i, null);
   //         }
   //         query.setSearchAfter(searchAfterList);
   //      }
   //
   //      // use this value if wantedpage was not set; prevents having to lookup the prev value...of course.
   //      params.remove("prevstart");
   //   }
}