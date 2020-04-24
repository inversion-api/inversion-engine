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
package io.inversion;

import io.inversion.utils.Utils;

/**
 *
 */
public abstract class Action<A extends Action> extends Rule<A>
{
   public void run(Request req, Response res) throws Exception
   {
      if (req.isGet())
         doGet(req, res);
      else if (req.isPost())
         doPost(req, res);
      else if (req.isPut())
         doPut(req, res);
      else if (req.isPatch())
         doPatch(req, res);
      else if (req.isDebug())
         doDelete(req, res);
   }

   public void doGet(Request req, Response res) throws Exception
   {
      ApiException.throw501NotImplemented("Either exclude GET requests for this Action in your Api configuration or override run() or doGet().");
   }

   public void doPost(Request req, Response res) throws Exception
   {
      ApiException.throw501NotImplemented("Either exclude POST requests for this Action in your Api configuration or override run() or doPost().");
   }

   public void doPut(Request req, Response res) throws Exception
   {
      ApiException.throw501NotImplemented("Either exclude PUT requests for this Action in your Api configuration or override run() or doPut().");
   }

   public void doPatch(Request req, Response res) throws Exception
   {
      ApiException.throw501NotImplemented("Either exclude PATCH requests for this Action in your Api configuration or override run() or doPatch().");
   }

   public void doDelete(Request req, Response res) throws Exception
   {
      ApiException.throw501NotImplemented("Either exclude DELETE requests for this Action in your Api configuration or override run() or doDelete().");
   }

   public String toString()
   {
      if (name != null)
         return name;

      String cn = getClass().getSimpleName();
      if (Utils.empty(cn))
         cn = getClass().getName();

      return cn;
   }
}
