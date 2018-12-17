package io.rcktapp.rql;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.rcktapp.api.Rule;

public class TestRule
{
   @Test
   public void testRulePathMatches()
   {
      assertTrue(Rule.pathMatches("*", "/something/asdfas/"));
      assertTrue(Rule.pathMatches("*", "something/asdfas/"));
      assertTrue(Rule.pathMatches("something/{collection:books|customers}", "something/books"));
      assertTrue(Rule.pathMatches("something/{collection:books|customers}", "something/Books"));
      assertTrue(Rule.pathMatches("something/{collection:books|customers}", "something/customers"));
      assertFalse(Rule.pathMatches("something/{collection:books|customers}", "something/blah"));
      assertTrue(Rule.pathMatches("something/{collection:books|customers}/*", "something/customers/1234"));

      assertTrue(Rule.pathMatches("something/{collection:books|customers}/{entity}", "something/customers/1234"));
      assertTrue(Rule.pathMatches("something/{collection:books|customers}/{entity:[0-9a-fA-F]{1,8}}", "something/customers/11111111"));
      assertTrue(Rule.pathMatches("something/{collection:books|customers}/{entity:[0-9a-fA-F]{1,8}}", "something/customers/aaaaaaaa"));
      assertFalse(Rule.pathMatches("something/{collection:books|customers}/{entity:[0-9a-fA-F]{1,8}}", "something/customers/aaaaaaaaaa"));
      assertFalse(Rule.pathMatches("something/{collection:books|customers}/{entity:[0-9a-fA-F]{1,8}}", "something/customers/1111111111"));
      assertFalse(Rule.pathMatches("something/{collection:books|customers}/{entity:[0-9a-fA-F]{1,8}}",  "something/customers/zzzzzzzz"));      

      assertTrue(Rule.pathMatches("something/{collection:books|customers}/{entity:[0-9]{1,8}}/{relationship:[a-zA-Z]*}", "something/customers/1234/orders"));
      assertTrue(Rule.pathMatches("something/{collection:books|customers}/{entity:[0-9]{1,8}}/{relationship:[a-zA-Z]*}", "something/customers/1234/orders/"));
      assertTrue(Rule.pathMatches("something/{collection:books|customers}/[{entity:[0-9]{1,8}}]/[{relationship:[a-zA-Z]*}]", "something/customers/1234/"));
      assertFalse(Rule.pathMatches("something/{collection:books|customers}/{entity:[0-9]{1,8}}/{relationship:[a-zA-Z]*}", "something/customers/1234/"));
      
      
      assertTrue(Rule.pathMatches("{collection:players|locations|ads}/[{entity:[0-9]{1,12}}]/{relationship:[a-z]*}", "Locations/698/players"));
      

   }
}
