package io.rocketpartners.cloud.rql;

import io.rocketpartners.cloud.service.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.JSObject;

public class RequestBuilder extends Term
{
   Service service = null;
   String  method  = null;
   String  url     = null;
   String  body    = null;

   public RequestBuilder(Service service, String method, String url, JSObject body)
   {
      this(service, method, url, body.toString());
   }

   public RequestBuilder(Service service, String method, String url, String body)
   {
      super(null, "ROOT");
      this.service = service;
      withMethod(method);
      withUrl(url);
      withBody(body);
   }

   public Response go()
   {
      String url = this.url;
      if (url.indexOf("?") < 0)
         url += "?";
      else if (!url.endsWith("&"))
         url += "&";

      for (int i = 0; i < getNumTerms(); i++)
      {
         url += getTerm(i).toString();
         if (i < getNumTerms() - 1)
            url += "&";
      }

      return service.service(method, url, body);
   }

   public String getMethod()
   {
      return method;
   }

   public RequestBuilder withMethod(String method)
   {
      this.method = method;
      return this;
   }

   public String getUrl()
   {
      return url;
   }

   public RequestBuilder withUrl(String url)
   {
      this.url = url;
      return this;
   }

   public Object getBody()
   {
      return body;
   }

   public RequestBuilder withBody(String body)
   {
      this.body = body;
      return this;
   }


}
