package io.rocketpartners.cloud.utils;

import io.rocketpartners.cloud.model.JsonMap;
import io.rocketpartners.cloud.utils.HttpUtils.FutureResponse;

public class Http
{
   public FutureResponse get(String url)
   {
      return HttpUtils.get(url);
   }

   public FutureResponse delete(String url)
   {
      return HttpUtils.delete(url);
   }

   public FutureResponse post(String url, JsonMap body)
   {
      return HttpUtils.post(url, body.toString());
   }

   public FutureResponse put(String url, JsonMap body)
   {
      return HttpUtils.put(url, body.toString());
   }

   public FutureResponse request(String method, String url, JsonMap body)
   {
      return HttpUtils.rest(method, url, body.toString(), null, 0);
   }

}
