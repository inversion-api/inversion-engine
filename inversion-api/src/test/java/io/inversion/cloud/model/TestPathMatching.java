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
package io.inversion.cloud.model;

import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import io.inversion.cloud.action.misc.MockAction;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;
import junit.framework.TestCase;

public class TestPathMatching extends TestCase
{
   //   @Test
   //   public void testRulePathMatches()
   //   {
   //      assertTrue(Rule.pathMatches("[{^$}]", ""));
   //      
   //      assertTrue(Rule.pathMatches("*", "/something/asdfas/"));
   //      assertTrue(Rule.pathMatches("*", "something/asdfas/"));
   //      assertTrue(Rule.pathMatches("something/{collection:books|customers}", "something/books"));
   //      assertTrue(Rule.pathMatches("something/{collection:books|customers}", "something/Books"));
   //      assertTrue(Rule.pathMatches("something/{collection:books|customers}", "something/customers"));
   //      assertFalse(Rule.pathMatches("something/{collection:books|customers}", "something/blah"));
   //      assertTrue(Rule.pathMatches("something/{collection:books|customers}/*", "something/customers/1234"));
   //
   //      assertTrue(Rule.pathMatches("something/{collection:books|customers}/{entity:[0-9a-fA-F]{1,8}}", "something/customers/11111111"));
   //      assertTrue(Rule.pathMatches("something/{collection:books|customers}/{entity:[0-9a-fA-F]{1,8}}", "something/customers/aaaaaaaa"));
   //      assertFalse(Rule.pathMatches("something/{collection:books|customers}/{entity:[0-9a-fA-F]{1,8}}", "something/customers/aaaaaaaaaa"));
   //      assertFalse(Rule.pathMatches("something/{collection:books|customers}/{entity:[0-9a-fA-F]{1,8}}", "something/customers/1111111111"));
   //      assertFalse(Rule.pathMatches("something/{collection:books|customers}/{entity:[0-9a-fA-F]{1,8}}",  "something/customers/zzzzzzzz"));      
   //
   //      assertTrue(Rule.pathMatches("something/{collection:books|customers}/{entity:[0-9]{1,8}}/{relationship:[a-zA-Z]*}", "something/customers/1234/orders"));
   //      assertTrue(Rule.pathMatches("something/{collection:books|customers}/{entity:[0-9]{1,8}}/{relationship:[a-zA-Z]*}", "something/customers/1234/orders/"));
   //      assertTrue(Rule.pathMatches("something/{collection:books|customers}/[{entity:[0-9]{1,8}}]/[{relationship:[a-zA-Z]*}]", "something/customers/1234/"));
   //      assertFalse(Rule.pathMatches("something/{collection:books|customers}/{entity:[0-9]{1,8}}/{relationship:[a-zA-Z]*}", "something/customers/1234/"));
   //      
   //      assertTrue(Rule.pathMatches("{collection:players|locations|ads}/[{entity:[0-9]{1,12}}]/{relationship:[a-z]*}", "Locations/698/players"));
   //   }

   @Test
   public void testEndpointMatch()
   {
      Endpoint ep = new Endpoint("GET", "blah", "books/*,cooks,cats,dogs/puppies");
      assertTrue(ep.matches("GET", "blah/books"));
      assertTrue(ep.matches("GET", "/blah/books/aasdf"));
      assertTrue(ep.matches("GET", "/blah/cats/"));
      assertFalse(ep.matches("GET", "/blah/cats/asdf"));
      assertFalse(ep.matches("GET", "/blah/dogs/"));
      assertFalse(ep.matches("GET", "/blah/dogs/asdf"));
      assertTrue(ep.matches("GET", "/blah/dogs/puppies"));
      assertFalse(ep.matches("GET", "/blah/dogs/puppies/asdf"));

      assertFalse(ep.matches("GET", "/blah/asdfasdf/dogs/puppies/asdf"));

      ep = new Endpoint("GET", "blah", "dogs/puppies");
      assertFalse(ep.matches("GET", "/blah/asdfasdf/dogs/puppies/asdf"));

      //this does not match because the constructor strips the /* as it conflicst with the fact 
      //that you passed in an includePaths
      ep = new Endpoint("GET", "blah/*", "dogs/puppies");
      assertFalse(ep.matches("GET", "/blah/asdfasdf/dogs/puppies/asdf"));
      assertTrue(ep.matches("GET", "/blah/dogs/puppies"));
      assertFalse(ep.matches("GET", "/blah/dogs/puppies/asdf"));

      ep = new Endpoint("GET", "blah", "*/dogs/puppies/*");
      assertTrue(ep.matches("GET", "/blah/asdfasdf/dogs/puppies/asdf"));

      ep = new Endpoint("GET", null, "books");
      assertTrue(ep.matches("GET", "books"));
      assertFalse(ep.matches("GET", "books/asdf"));

      ep = new Endpoint("GET", null, "books/*,cooks,cats,dogs/puppies");
      assertTrue(ep.matches("GET", "books"));
      assertTrue(ep.matches("GET", "books/asdf"));

      ep = new Endpoint("GET", "/", "books/*,cooks,cats,dogs/puppies");
      assertTrue(ep.matches("GET", "books"));
      assertTrue(ep.matches("GET", "books/asdf"));

      ep = new Endpoint("GET", "", "books/*,cooks,cats,dogs/puppies");
      assertTrue(ep.matches("GET", "books"));
      assertTrue(ep.matches("GET", "books/asdf"));
      assertFalse(ep.matches("GET", "asdf/books/asdf"));
   }

   @Test
   public void testApiEndpointPathMatching()
   {
      Api api0 = new Api()//
                          .withEndpoint("GET", "endpoint_path/*", null, "ep0", new MockAction("all"));

      //if you only have one API, you can leave the API code null...you can only really do this from code configured APIs not prop wired APIs
      assertEndpointMatch("GET", "http://localhost/endpoint_path/12345", 200, "ep0", "endpoint_path", "12345", null, null, api0);

      Api api1 = new Api("test")//
                                .withAction(new MockAction("mock1").withIncludePaths("*"))//
                                .withEndpoint("GET", "ep1/*", null, "ep1")//
                                .withEndpoint("GET", "ep2/", null, "ep2")//
                                .withEndpoint("GET", "bookstore/", "books/*,categories,authors", "ep3")//
                                .withEndpoint("GET", "other/data", "table1,table2/*,other/data/*,data/*", "ep4")//
                                .withEndpoint("GET", "cardealer", "ford/*,gm/*", "ep5")//
                                .makeEndpoint("GET", "petstore/*", null, "ep6").withExcludePaths("rat", "snakes/bad", "cats/*").getApi()//
                                .withEndpoint("GET", "gamestop/*", "nintendo,xbox/*", "ep7")//
                                .withEndpoint("GET", "carwash", "regular,delux/*", "ep8");

      Api api2 = new Api("other");

      assertEndpointMatch("GET", "http://localhost/test/ep1", 200, api1);
      assertEndpointMatch("GET", "/test/ep1", 200, api1);
      assertEndpointMatch("GET", "test/ep1", 200, api1);
      assertEndpointMatch("GET", "http://localhost/WRONG/ep1", 404, api1);
      assertEndpointMatch("GET", "http://localhost/WRONG/ep1", 404, api1, api2);

      assertEndpointMatch("GET", "http://localhost/test/ep1/collKey/entKey/relKey", 200, "ep1", "ep1", "collKey", "entKey", "relKey", api1);
      assertEndpointMatch("GET", "http://localhost/test/ep1/collKey/entKey/relKey", 500, "ep1", "ep1", "collKey", "entKey", "asdfasd", api1);
      assertEndpointMatch("DELETE", "http://localhost/test/ep1/collKey/entKey/relKey", 404, api1);

      assertEndpointMatch("GET", "test/ep2", 200, api1);
      assertEndpointMatch("GET", "http://localhost/test/ep2/", 200, api1);
      assertEndpointMatch("GET", "http://localhost/test/ep2/asdf", 404, api1);

      assertEndpointMatch("GET", "http://localhost/test/bookstore/books/1/author", 200, "ep3", "bookstore", "books", "1", "author", api1);
      assertEndpointMatch("GET", "http://localhost/test/bookstore/categories/fiction/books", 404, "ep3", "bookstore", "categories", "fiction", "books", api1);
      assertEndpointMatch("GET", "http://localhost/test/bookstore/cars/", 404, api1);

      assertEndpointMatch("GET", "/test/other/data/table1/", 200, "ep4", "other/data", "table1", null, null, api1);
      assertEndpointMatch("GET", "http://localhost/test/other/data/table1/asdfa/", 404, api1);
      assertEndpointMatch("GET", "http://localhost/test/other/data/table2/keyCol/relCol", 200, "ep4", "other/data", "table2", "keyCol", "relCol", api1);
      assertEndpointMatch("GET", "test/other/data/data/keyCol/relCol", 200, "ep4", "other/data", "data", "keyCol", "relCol", api1);
      assertEndpointMatch("GET", "http://localhost/test/other/data/other/data/relCol", 200, "ep4", "other/data", "other", "data", "relCol", api1);

      assertEndpointMatch("GET", "/test/cardealer/ford/explorer", 200, "ep5", "cardealer", "ford", "explorer", null, api1);
      assertEndpointMatch("GET", "/test/cardealer/gm", 200, "ep5", "cardealer", "gm", null, null, api1);
      assertEndpointMatch("GET", "/test/cardealer/ford/toyota", 404);

      assertEndpointMatch("GET", "/test/petstore/dogs/1234/breed", 200, "ep6", "petstore", "dogs", "1234", "breed", api1);
      assertEndpointMatch("GET", "/test/petstore/rat/", 404, api1);
      assertEndpointMatch("GET", "/test/petstore/rat/a_rat", 200, "ep6", "petstore", "rat", "a_rat", null, api1);
      assertEndpointMatch("GET", "/test/petstore/snakes/good", 200, "ep6", "petstore", "snakes", "good", null, api1);
      assertEndpointMatch("GET", "/test/petstore/snakes/bad/", 404, api1);
      assertEndpointMatch("GET", "/test/petstore/snakes/bad/butgood", 200, "ep6", "petstore", "snakes", "bad", "butgood", api1);
      assertEndpointMatch("GET", "/test/petstore/cats/", 404, api1);
      assertEndpointMatch("GET", "/test/petstore/cats/nope", 404, api1);
      assertEndpointMatch("GET", "/test/petstore/cats/nope/none", 404, api1);

      //the Endpoint constructor will strip /* from gamestop/* because it conflicts with having passed in includePaths
      assertEndpointMatch("GET", "/test/gamestop/nintendo", 200, "ep7", "gamestop", "nintendo", null, null, api1);
      assertEndpointMatch("GET", "/test/gamestop/nintendo/game", 404, api1);
      assertEndpointMatch("GET", "/test/gamestop/xbox/somegame", 200, "ep7", "gamestop", "xbox", "somegame", null, api1);
      assertEndpointMatch("GET", "/test/gamestop/nintendo/sega", 404, api1);

   }

   void assertEndpointMatch(String method, String url, int statusCode, Api... apis)
   {
      assertEndpointMatch(method, url, statusCode, null, null, null, null, null, apis);
   }

   void assertEndpointMatch(String method, String url, int statusCode, String endpointName, String endpointPath, String collectionKey, String entityKey, String subCollectionKey, Api... apis)
   {
      final boolean[] success = new boolean[]{false};
      Engine e = new Engine()
         {
            protected void service(Chain chain, List<Action> actions) throws Exception
            {
               if (endpointName != null && !endpointName.equals(chain.getRequest().getEndpoint().getName()))
                  fail(chain, "endpoints don't match");

               if (endpointPath != null && !endpointPath.equals(chain.getRequest().getEndpointPath().toString()))
               {
                  fail(chain, "endpoint path doesn't match");
               }

               if (collectionKey != null && !collectionKey.equals(chain.getRequest().getCollectionKey()))
                  fail(chain, "collectionKey don't match");

               if (entityKey != null && !entityKey.equals(chain.getRequest().getEntityKey()))
                  fail(chain, "entityKey don't match");

               if (subCollectionKey != null && !subCollectionKey.equals(chain.getRequest().getSubCollectionKey()))
                  fail(chain, "subCollectionKey don't match");

               success[0] = true;
            }

            protected void fail(Chain chain, String message, Object... vals)
            {
               System.err.print(message);
               for (int i = 0; vals != null && i < vals.length; i++)
                  System.err.print(vals[i] + " ");
               System.err.println("");

               System.err.println("url              :" + chain.getRequest().getUrl());
               System.err.println("apiUrl           :" + chain.getRequest().getApiUrl());
               System.err.println("endpoint         :" + chain.getRequest().getEndpoint());
               System.err.println("ep path          :" + chain.getRequest().getEndpointPath());
               System.err.println("collectionKey    :" + chain.getRequest().getCollectionKey());
               System.err.println("entityKey        :" + chain.getRequest().getEntityKey());
               System.err.println("subCollectionKey :" + chain.getRequest().getSubCollectionKey());
               System.err.println("subPath          :" + chain.getRequest().getSubpath());

               throw new RuntimeException(message);
            }
         };

      for (Api api : apis)
      {
         if (api != null)
            e.withApi(api);//without this additional API, any apiCode will match
      }

      Response resp = e.service(method, url);
      resp.dump();

      if (statusCode != resp.getStatusCode())
         fail("failed");

      if (statusCode < 400 && !success[0])
         fail("failed");
   }

}
