package io.inversion.cloud.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.inversion.cloud.service.MockActionA;

public class TestEndpoint 
{
   @Test
   public void testEndpointMatching1()
   {
      Endpoint ep = null;

      ep = new Endpoint("GET", "actionA*", new MockActionA("GET", "*"));
      assertEquals("actionA*", ep.getIncludePaths().get(0).toString());
      assertTrue(ep.matches("GET", "actionA"));
      assertFalse(ep.matches("PUT", "actionA"));
      assertFalse(ep.matches("GET", "actionB"));
      assertTrue(ep.matches("GET", "actionAasdf"));
      assertFalse(ep.matches("GET", "actionA/asdasdf"));

      ep = new Endpoint("GET", "actionA", new MockActionA("GET", "*"));
      assertEquals("actionA", ep.getPath().toString());
      assertEquals(0, ep.getIncludePaths().size());
      assertTrue(ep.matches("GET", "actionA"));
      assertFalse(ep.matches("GET", "actionA/asdf"));

      ep = new Endpoint("GET", "actionA/*", new MockActionA("GET", "*"));
      assertEquals("actionA", ep.getPath().toString());
      assertEquals("*", ep.getIncludePaths().get(0).toString());
      assertTrue(ep.matches("GET", "actionA/asdf"));

      ep = new Endpoint("GET", "actionA/asdf*", new MockActionA("GET", "*"));
      assertEquals("actionA", ep.getPath().toString());
      assertEquals("asdf*", ep.getIncludePaths().get(0).toString());
      assertTrue(ep.matches("GET", "actionA/asdf"));
      assertFalse(ep.matches("GET", "actionA/zxcvasdf"));

      ep = new Endpoint("GET", "actionA", new MockActionA("GET", "*")).withExcludePaths("*");
      assertEquals("actionA", ep.getPath().toString());
      assertTrue(ep.matches("GET", "actionA/"));
      assertTrue(ep.matches("GET", "actionA"));
      assertFalse(ep.matches("GET", "actionA/zxcvasdf"));

      ep = new Endpoint("GET", "actionA", new MockActionA("GET", "*")).withExcludePaths("asdf*");
      assertEquals("actionA", ep.getPath().toString());
      assertEquals("asdf*", ep.getExcludePaths().get(0).toString());
      assertFalse(ep.matches("GET", "actionA/asdf"));

      ep = new Endpoint("GET", "actionA/*", new MockActionA("GET", "*")).withExcludePaths("bbb*,ccc*,ddd,aaa");
      assertEquals("actionA", ep.getPath().toString());
      assertEquals("*", ep.getIncludePaths().get(0).toString());
      assertEquals("bbb*", ep.getExcludePaths().get(0).toString());
      assertEquals("ccc*", ep.getExcludePaths().get(1).toString());
      assertEquals("ddd", ep.getExcludePaths().get(2).toString());
      assertEquals("aaa", ep.getExcludePaths().get(3).toString());
      assertFalse(ep.matches("GET", "actionA/aaa"));
      assertTrue(ep.matches("GET", "actionA/aaaxyz"));
      assertFalse(ep.matches("GET", "actionA/ddd"));
      assertTrue(ep.matches("GET", "actionA/dddxyz"));

      assertFalse(ep.matches("GET", "actionA/bbb"));
      assertFalse(ep.matches("GET", "actionA/bbbsdfbshtgzdsfg"));
      assertFalse(ep.matches("GET", "actionA/ccc"));
      assertFalse(ep.matches("GET", "actionA/cccccccasdfasdf"));
      assertTrue(ep.matches("GET", "actionA/dddddd"));

      ep = new Endpoint("GET", "actionA/aaa*", new MockActionA("GET", "*")).withExcludePaths("bbb*,ccc*,ddd,aaa");
      assertEquals("actionA", ep.getPath().toString());
      assertEquals("aaa*", ep.getIncludePaths().get(0).toString());
      assertEquals("bbb*", ep.getExcludePaths().get(0).toString());
      assertEquals("ccc*", ep.getExcludePaths().get(1).toString());
      assertEquals("ddd", ep.getExcludePaths().get(2).toString());
      assertEquals("aaa", ep.getExcludePaths().get(3).toString());
      assertFalse(ep.matches("GET", "actionA/aaa"));
      assertTrue(ep.matches("GET", "actionA/aaaxyz"));
      assertFalse(ep.matches("GET", "actionA/"));//not allowed or denied so denied
      assertFalse(ep.matches("GET", "actionA/abcd"));//not allowed or denied so denied
      assertFalse(ep.matches("GET", "actionA/bb"));//not allowed or denied so denied
      assertFalse(ep.matches("GET", "actionA/ccc"));//specifically denied
   }

   @Test
   public void test_endpoint_matchs()
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
}
