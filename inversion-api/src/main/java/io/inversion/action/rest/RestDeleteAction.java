/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.inversion.action.rest;

import java.util.HashSet;
import java.util.Set;

import io.inversion.Action;
import io.inversion.ApiException;
import io.inversion.Collection;
import io.inversion.Engine;
import io.inversion.Request;
import io.inversion.Response;
import io.inversion.Status;
import io.inversion.utils.JSNode;
import io.inversion.utils.Rows;
import io.inversion.utils.Utils;
import io.inversion.utils.Rows.Row;

public class RestDeleteAction extends Action<RestDeleteAction>
{
   public RestDeleteAction()
   {
      withMethods("DELETE");
   }

   @Override
   public void run(Request req, Response res) throws Exception
   {
      String entityKey = req.getEntityKey();
      String relationshipKey = req.getRelationshipKey();
      
      if (Utils.empty(entityKey))
         ApiException.throw400BadRequest("An entity key must be included in the url path for a DELETE request.");
      
      if (!Utils.empty(relationshipKey))
         ApiException.throw400BadRequest("A relationship key in the url path is not valid for a DELETE request");

      if (req.getJson() != null)
         ApiException.throw501NotImplemented("A JSON body can not be included with a DELETE.  Batch delete is not supported.");

      int deleted = delete(req.getEngine(), req.getCollection(), req.getUrl().toString());

      if (deleted < 1)
         res.withStatus(Status.SC_404_NOT_FOUND);
      else
         res.withStatus(Status.SC_204_NO_CONTENT);
   }

   protected int delete(Engine engine, Collection collection, String url) throws Exception
   {
      int deleted = 0;

      Set alreadyDeleted = new HashSet();

      for (int i = 0; i < 1000; i++)
      {
         //-- regardless of the query string passed in, this should resolve the keys 
         //-- that need to be deleted and make sure the uses has read access to the key
         Response res = engine.get(url).assertStatus(200, 404);

         if (res.getData().size() == 0)
            break;

         Rows rows = new Rows();

         for (JSNode node : res.getData().asNodeList())
         {
            String href = node.getString("href");

            if (alreadyDeleted.contains(href))
               ApiException.throw500InternalServerError("Deletion of '{}' was not successful.", href);
            else
               alreadyDeleted.add(href);

            Row key = collection.decodeKey((String) Utils.last(Utils.explode("/", href)));
            rows.add(key);
         }
         collection.getDb().delete(collection, rows);

         deleted += res.getData().size();
      }

      return deleted;
   }
}
