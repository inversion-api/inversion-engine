/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.inversion.cloud.action.misc;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;

/**
 * This endpoint does not do anything but return the configured HTTP status, defaulted to 200.
 * This is useful when you need to setup an automated healthcheck for a service.  For example, 
 * if you are configuring a CICD pipeline that checks the health of a deployment, you might configure
 * this at path GET /status or something like that for the deployer to check for correct deployment.
 * 
 * @author wells
 *
 */
public class StatusAction extends Action<StatusAction>
{
   protected int     statusCode    = 200;
   protected String  status        = SC.SC_200_OK;
   protected boolean cancelRequest = true;

   public StatusAction()
   {
      this(null);
   }

   public StatusAction(String inludePaths)
   {
      this(inludePaths, null, null);
   }

   public StatusAction(String inludePaths, String excludePaths, String config)
   {
      super(inludePaths, excludePaths, config);
   }

   public void run(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      res.withStatus(status);
      res.withStatusCode(statusCode);
      if (cancelRequest)
         chain.cancel();
   }

   public int getStatusCode()
   {
      return statusCode;
   }

   public StatusAction StatusCode(int statusCode)
   {
      this.statusCode = statusCode;
      return this;
   }

   public String getStatus()
   {
      return status;
   }

   public StatusAction withStatus(String status)
   {
      this.status = status;
      return this;
   }

   public boolean isCancelRequest()
   {
      return cancelRequest;
   }

   public StatusAction withCancelRequest(boolean cancelRequest)
   {
      this.cancelRequest = cancelRequest;
      return this;
   }

}
