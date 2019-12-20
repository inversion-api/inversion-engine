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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author kfrankic
 *
 */
public class Term extends ElasticQuery
{

   @JsonIgnore
   private String name;

   @JsonIgnore
   private List<Object> valueList;

   // the token used to create this Term. Distinguishes between 'filters' and 'must_nots' 
   @JsonIgnore
   private String token;

   @JsonIgnore
   private Float  boost;
   
   public Term(String name, String token)
   {
      this.name = name;
      this.valueList = null;

      this.token = token;

      if (name.contains("."))
      {
         this.nestedPath = name.substring(0, name.lastIndexOf("."));
      }
   }

   public Term(String name, Object value, String token)
   {
      this.name = name;
      this.valueList = new ArrayList<Object>();
      valueList.add(value);
      this.token = token;

      if (name.contains("."))
      {
         this.nestedPath = name.substring(0, name.lastIndexOf("."));
      }
   }
   


   /**
    * This method is necessary to output Json in the proper format. The 
    * format is the following:
    * 
    *  {
    *       "nameValue":"valueValue",
    *       "boost":2.0
    *  }
    *  
    * @return
    */
   @JsonAnyGetter
   public Map<String, Object> any()
   {
      Map<String, Object> properties = new HashMap<String, Object>();

      if (isTerm())
         properties.put(name, valueList.get(0));
      else
         properties.put(name, valueList);

      if (boost != null)
         properties.put("boost", boost);

      return properties;
   }
   
   @JsonIgnore
   public boolean isTerm() {
      if (valueList != null && valueList.size() > 1)
         return false;
      return true;
   }

   /**
    * @param boost the boost to set
    */
   public void setBoost(Float boost)
   {
      this.boost = boost;
   }

   /**
    * @return the name
    */
   public String getName()
   {
      return name;
   }

   /**
    * @return the value
    */
   public List<Object> getValueList()
   {
      return valueList;
   }

   /**
    * @return the boost
    */
   public Float getBoost()
   {
      return boost;
   }

   /**
    * @return the token
    */
   public String getToken()
   {
      return token;
   }
   
   public void addValue(Object value) {
      if (valueList == null)
         valueList = new ArrayList<Object>();
      
      valueList.add(value);
   }
   
   public void addValueList(List<Object> valueList) {
      if(valueList == null)
         this.valueList = new ArrayList<Object>(valueList);
      else
         this.valueList.addAll(valueList);
   }
}
