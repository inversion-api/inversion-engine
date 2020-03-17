/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.model;

import java.util.ArrayList;
import java.util.List;

public class Endpoint extends Rule<Endpoint>
{
   protected List<Action> actions  = new ArrayList();

   /**
    * Internal endpoints can only be called by recursive 
    * calls to the engine when Chain.depth() is > 1.  
    */
   protected boolean      internal = false;

   public Endpoint()
   {

   }

   public Endpoint(String method, String includePaths, Action... actions)
   {
      withMethods(method);
      withIncludePaths(includePaths);

      if (actions != null)
      {
         for (Action action : actions)
            withAction(action);
      }
   }

   public Endpoint withInternal(boolean internal)
   {
      this.internal = internal;
      return this;
   }

   public List<Action> getActions()
   {
      return new ArrayList(actions);
   }

   public Endpoint withActions(Action... actions)
   {
      for (Action action : actions)
         withAction(action);

      return this;
   }

   public Endpoint withAction(Action action)
   {
      if (actions.contains(action))
         return this;

      boolean inserted = false;
      for (int i = 0; i < actions.size(); i++)
      {
         if (action.getOrder() < actions.get(i).getOrder())
         {
            actions.add(i, action);
            inserted = true;
            break;
         }
      }

      if (!inserted)
         actions.add(action);

      return this;
   }

}
