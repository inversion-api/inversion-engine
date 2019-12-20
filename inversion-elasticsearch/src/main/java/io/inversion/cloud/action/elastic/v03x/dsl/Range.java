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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The class properties are all annotated with JsonIgnore so that the 
 * Jackson ObjectMapper will use the JsonAnyGetter method instead.  
 * This is done because a specific format is expected for this class.
 * 
 * @author kfrankic
 *
 */
public class Range extends ElasticQuery
{

   // The name of the elasticSearch field 
   @JsonIgnore
   private String name;

   @JsonIgnore
   private Object gte;
   @JsonIgnore
   private Object gt;
   @JsonIgnore
   private Object lte;
   @JsonIgnore
   private Object lt;
   @JsonIgnore
   private Object boost;

   public Range(String name)
   {
      this.name = name;
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
    *       "nameValue":{
    *           "gte":10,
    *           "lte":15,
    *           "boost":2.0
    *       }
    *  }
    *  
    * @return
    */
   @JsonAnyGetter
   public Map<String, Object> any()
   {
      Map<String, Object> properties = new HashMap<String, Object>();

      if (getGt() != null)
         properties.put("gt", getGt());

      if (getGte() != null)
         properties.put("gte", getGte());

      if (getLt() != null)
         properties.put("lt", getLt());

      if (getLte() != null)
         properties.put("lte", getLte());

      if (getBoost() != null)
         properties.put("boost", getBoost());

      return Collections.singletonMap(name, properties);
   }

   /**
    * @return the name
    */
   public String getName()
   {
      return name;
   }

   /**
    * @return the gte
    */
   public Object getGte()
   {
      return gte;
   }

   /**
    * @param gte the gte to set
    */
   public void setGte(Object gte)
   {
      this.gte = gte;
   }

   /**
    * @return the gt
    */
   public Object getGt()
   {
      return gt;
   }

   /**
    * @param gt the gt to set
    */
   public void setGt(Object gt)
   {
      this.gt = gt;
   }

   /**
    * @return the le
    */
   public Object getLte()
   {
      return lte;
   }

   /**
    * @param lte the lte to set
    */
   public void setLte(Object lte)
   {
      this.lte = lte;
   }

   /**
    * @return the lt
    */
   public Object getLt()
   {
      return lt;
   }

   /**
    * @param lt the lt to set
    */
   public void setLt(Object lt)
   {
      this.lt = lt;
   }

   /**
    * @return the boost
    */
   public Object getBoost()
   {
      return boost;
   }

   /**
    * @param boost the boost to set
    */
   public void setBoost(Object boost)
   {
      this.boost = boost;
   }

}
