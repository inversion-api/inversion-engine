package io.rocketpartners.cloud.action.misc;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Engine;

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
