/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * http://rocketpartners.io
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
package io.rocketpartners.cloud.rql.elastic;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * In Elastic, the value property of a Fuzzy query only works with a single value.  Use a BoolQuery to 
 * fuzzy search multiple values of the same field.
 * @author kfrankic
 *
 */
public class FuzzyQuery extends ElasticQuery
{

   @JsonIgnore
   private String  name;

   @JsonIgnore
   private Object  value;

   @JsonIgnore
   private Float   boost;

   @JsonIgnore
   private Integer fuzziness;

   @JsonIgnore
   private Integer prefix_length;

   @JsonIgnore
   private Integer max_expansions;

   public FuzzyQuery(String name, Object value)
   {
      this.name = name;
      this.value = value;

      if (name.contains("."))
      {
         this.nestedPath = name.substring(0, name.lastIndexOf("."));
      }
   }

   //   /**
   //    * Not used atm as it could lead to query issues if prefix_len is set to 0 and max_expansions is set
   //    * to a high number.  It could result in every term in the index being examined.
   //    */
   //   public FuzzyQuery(String name, Object value, Float boost, Integer fuzziness, Integer prefix_len, Integer max_expansions)
   //   {
   //      this.name = name;
   //      this.value = value;
   //      this.boost = boost;
   //      this.fuzziness = fuzziness;
   //      this.prefix_length = prefix_len;
   //      this.max_expansions = max_expansions;
   //
   //      if (name.contains("."))
   //      {
   //         this.nestedPath = name.substring(0, name.lastIndexOf("."));
   //      }
   //   }

   /**
    * This method is necessary to output Json in the proper formats. The 
    * formats are the following:
    * 
    *  {
    *       "nameValue": valueValue
    *  }
    *  
    *  or
    *  
    *  {
    *       "nameValue":{
    *           "value": valueValue,
    *           "boost": boostValue,
    *           "fuzziness": fuzziValue,
    *           "prefix_length": preValue,
    *           "max_expansions": maxValue
    *       }
    *  }
    *  
    * @return
    */
   @JsonAnyGetter
   public Map<String, Object> any()
   {
      Map<String, Object> properties = new HashMap<String, Object>();

      if (boost == null && fuzziness == null && prefix_length == null && max_expansions == null)
         properties.put(name, value);
      else
      {
         Map<String, Object> nameProps = new HashMap<String, Object>();

         nameProps.put("value", value);

         if (boost != null)
            nameProps.put("boost", boost);

         if (fuzziness != null)
            nameProps.put("fuzziness", fuzziness);

         if (prefix_length != null)
            nameProps.put("prefix_length", prefix_length);

         if (max_expansions != null)
            nameProps.put("max_expansions", max_expansions);

         properties.put(name, nameProps);
      }

      return properties;
   }

}
