/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.action.elastic.v03x.dsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Very basic Elastic sorting.  Using a class for this since it will eventually be fleshed-out.
 * Obviously, nested-sorting is not handled atm.
 * @author kfrankic
 *
 */
public class Order
{
   @JsonIgnore
   private List<Map<String, String>> orderList;

   public Order(String name, String order)
   {
      orderList = new ArrayList<Map<String, String>>();
      addOrder(name, order);
   }

   public void addOrder(String name, String order)
   {
      Map<String, String> map = new HashMap<String, String>();
      map.put(name, order);
      orderList.add(map);
   }

   public void reverseOrdering()
   {
      for (int i = 0; i < orderList.size(); i++)
      {
         Map<String, String> orderMap = orderList.get(i);
         for (Map.Entry<String, String> entry : orderMap.entrySet())
         {
            if (entry.getValue().equalsIgnoreCase("ASC"))
            {
               entry.setValue("DESC");
            }
            else
            {
               entry.setValue("ASC");
            }

         }
      }
   }

   /**
    * @return the orderList
    */
   public List<Map<String, String>> getOrderList()
   {
      return orderList;
   }

   /**
    * Converts a list of order object [{id1, desc}, {id2, asc}] to 
    * the following format [-id1,id2]
    * @return
    */
   public List<String> getOrderAsStringList()
   {
      List<String> list = new ArrayList<String>();

      for (Map<String, String> map : orderList)
      {
         for (Map.Entry<String, String> entry : map.entrySet())
         {
            if (entry.getValue().equalsIgnoreCase("DESC"))
               list.add("-" + entry.getKey());
            else
               list.add(entry.getKey());
         }
      }

      return list;
   }

}
