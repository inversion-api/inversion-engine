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

import org.junit.Test;

import junit.framework.TestCase;

public class TestRule extends TestCase
{
   @Test
   public void test_pathMatches()
   {
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
      //      assertFalse(Rule.pathMatches("something/{collection:books|customers}/{entity:[0-9a-fA-F]{1,8}}", "something/customers/zzzzzzzz"));
      //
      //      assertTrue(Rule.pathMatches("something/{collection:books|customers}/{entity:[0-9]{1,8}}/{relationship:[a-zA-Z]*}", "something/customers/1234/orders"));
      //      assertTrue(Rule.pathMatches("something/{collection:books|customers}/{entity:[0-9]{1,8}}/{relationship:[a-zA-Z]*}", "something/customers/1234/orders/"));
      //      assertTrue(Rule.pathMatches("something/{collection:books|customers}/[{entity:[0-9]{1,8}}]/[{relationship:[a-zA-Z]*}]", "something/customers/1234/"));
      //      assertFalse(Rule.pathMatches("something/{collection:books|customers}/{entity:[0-9]{1,8}}/{relationship:[a-zA-Z]*}", "something/customers/1234/"));
      //
      //      assertTrue(Rule.pathMatches("{collection:players|locations|ads}/[{entity:[0-9]{1,12}}]/{relationship:[a-z]*}", "Locations/698/players"));
   }
}
