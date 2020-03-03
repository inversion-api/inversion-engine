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
package io.inversion.cloud.action.rest;

import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.*;
import io.inversion.cloud.model.Rows.Row;
import io.inversion.cloud.rql.RqlParser;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.utils.Utils;

import java.util.*;

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
      String subcollectionKey = req.getSubCollectionKey();
      JSNode json = req.getJson();

      int count = Utils.empty(entityKey) ? 0 : 1;
      count += Utils.empty(req.getQuery()) ? 0 : 1;
      count += json == null ? 0 : 1;

      if (count != 1)
         ApiException.throw400BadRequest("DELETE expects an entity url, OR a query string OR a JSON array of entity urls, but only one at a time.");

      if (!Utils.empty(subcollectionKey))
         ApiException.throw400BadRequest("A subcollection key is not valid for a DELETE request");

      List<String> toDelete = new ArrayList();

      if (req.getJson() != null)
      {
         if (!(json instanceof JSArray))
         {
            ApiException.throw400BadRequest("The JSON body to a DELETE must be an array that contains string urls.");
         }

         for (Object o : (JSArray) json)
         {
            if (!(o instanceof String))
               ApiException.throw400BadRequest("The JSON body to a DELETE must be an array that contains string urls.");

            String url = (String) o;

            String path = req.getUrl().toString();
            if (path.indexOf("?") > 0)
            {
               path = path.substring(0, path.indexOf("?") - 1);
            }

            if (!url.toLowerCase().startsWith(path.toLowerCase()))
            {
               ApiException.throw400BadRequest("All delete request must be for the collection in the original request: '" + path + "'");
            }
            toDelete.add((String) o);
         }
      }
      else if (entityKey != null)
      {
         toDelete.add(req.getUrl().toString());
      }
      else
      {
         toDelete.add(req.getUrl().toString());
      }

      String collectionUrl = req.getApiUrl();
      if (!collectionUrl.endsWith("/"))
         collectionUrl += "/";

      collectionUrl += Utils.implode("/", req.getEndpointPath().toString(), req.getCollectionKey().toString());
      int deleted = delete(req, req.getCollection(), collectionUrl, toDelete);

      if (deleted < 1)
         res.withStatus(Status.SC_404_NOT_FOUND);
      else
         res.withStatus(Status.SC_204_NO_CONTENT);
   }

   protected int delete(Request req, Collection collection, String collectionUrl, List<String> urls) throws Exception
   {
      int deleted = 0;
      //------------------------------------------------
      // Normalize all of the params and convert attribute
      // names to column names.

      //      List<Term> terms = new ArrayList();
      //      terms.add(Term.term(null, "eq", "includes", "href"));

      Term or = Term.term(null, "or");
      Term in = Term.term(null, "_key", req.getCollection().getPrimaryIndex().getName());

      RqlParser parser = new RqlParser();
      for (String u : urls)
      {
         Url url = new Url(u);
         Map<String, String> params = url.getParams();
         if (params.size() == 0)
         {
            List<String> path = url.getPath().parts();
            String key = path.get(path.size() - 1);
            in.withTerm(Term.term(in, key));
         }
         else
         {
            Term and = Term.term(null, "and");
            for (String paramName : params.keySet())
            {
               String termStr = null;
               String paramValue = params.get(paramName);

               if (Utils.empty(paramValue) && paramName.indexOf("(") > -1)
               {
                  termStr = paramName;
               }
               else
               {
                  termStr = "eq(" + paramName + "," + paramValue + ")";
               }
               Term term = parser.parse(termStr);
               and.withTerm(term);
            }
            if (and.size() == 0)
            {
               ApiException.throw400BadRequest("You can't DELETE to a collection unless you include an entityKey or query string");
            }
            else
            {
               or.withTerm(and);
            }
         }
      }

      Term query = in;
      if (or.size() > 0)
      {
         if (in.size() > 1)
            or.withTerm(in);

         if (or.size() == 1)
            query = or.getTerm(0);
         else
            query = or;

      }

      String url = collectionUrl + "?" + query + "&page=1&pageSize=100&includes=href";

      Set alreadyDeleted = new HashSet();

      for (int i = 0; i < 1000; i++)
      {

         //regardless of the query string passed in, this should resolve the keys 
         //that need to be deleted and make sure the uses has read access to the key
         //
         //TODO: need to do more tests here
         Response res = req.getEngine().get(url).assertOk();

         if (res.data().size() == 0)
            break;

         deleted += res.data().size();

         Rows rows = new Rows();

         for (JSNode node : res.data().asNodeList())
         {
            String href = node.getString("href");

            if (alreadyDeleted.contains(href))
               ApiException.throw500InternalServerError("Deletion of '%s' was not successful.", href);
            else
               alreadyDeleted.add(href);

            Row key = collection.decodeKey((String) Utils.last(Utils.explode("/", href)));
            rows.add(key);
         }

         //res.data().asList().forEach(o -> ));
         req.getCollection().getDb().delete(collection, rows);
      }

      return deleted;
   }
}
