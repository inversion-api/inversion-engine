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
package io.rcktapp.api;

import io.rcktapp.api.service.Service;

/**
 * An Action exists to parameterize the invocation of a Handler
 * and allow the Endpoint to order its list of handlers
 * 
 * @author wells
 */
public class Action extends Rule
{
   String  comment   = null;

   //JSObject config    = null;

   int     handlerId = 0;   //crutch for db persistance
   Handler handler   = null;

   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      if (handler == null)
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Action is missing a handler");

      //req.setEntityKey(action.getParam("entityKey", req.getEntityKey()));
      //req.setCollectionKey(action.getParam("collectionKey", req.getCollectionKey()));
      //req.setSubCollectionKey(action.getParam("subCollectionKey", req.getSubCollectionKey()));
      handler.service(service, api, endpoint, this, chain, req, res);
   }

   public Handler getHandler()
   {
      return handler;
   }

   public void setHandler(Handler handler)
   {
      this.handler = handler;
   }

   public void setHandlerId(int handlerId)
   {
      this.handlerId = handlerId;
   }

   public int getHandlerId()
   {
      return handlerId;
   }

   public String getComment()
   {
      return comment;
   }

   public void setComment(String comment)
   {
      this.comment = comment;
   }

}
