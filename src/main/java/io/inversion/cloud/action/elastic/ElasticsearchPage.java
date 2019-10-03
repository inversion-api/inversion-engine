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