/*
 * Copyright (c) 2015-2020 Rocket Partners, LLC
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
package io.inversion.cloud.rql;

import org.junit.Test;

import io.inversion.cloud.model.Db;
import io.inversion.cloud.service.Engine;
import junit.framework.TestCase;

public abstract class AbstractRqlTest extends TestCase
{
   protected String queryClass = null;
   protected String urlPrefix  = null;
   protected Engine engine     = null;
   protected Db     db         = null;

   @Test
   public void test_doSelect_unitTests() throws Exception
   {
      if (getClass().getName().indexOf("IntegTest") >= 0)
      {
         System.out.println("Skipping units tests...I have 'IntegTest' in my classname so I am skipping the unit tests expecting a diffent subclass without 'IntegTest' in the name is running those.");
         return;
      }

      RqlValidationSuite suite = new RqlValidationSuite(queryClass, db);
      customizeUnitTestSuite(suite);
      suite.runUnitTests();
   }

   @Test
   public void test_doSelect_integTests() throws Exception
   {
      if (getClass().getName().indexOf("IntegTest") < 0)
      {
         System.out.println("Skipping integ tests...subclasse me with 'IntegTest' in my classname to get me to run integ tests");
         return;
      }

      RqlValidationSuite suite = new RqlValidationSuite(queryClass, db);
      customizeIntegTestSuite(suite);
      suite.runIntegTests(engine, urlPrefix);
   }

   /**
    * This is here as a template that you can copy 
    */
   protected void customizeUnitTestSuite(RqlValidationSuite suite)
   {

      suite//
           .withResult("eq", "PUT YOUR SQL HERE")//
           .withResult("ne", "")//
           .withResult("n", "")//
           .withResult("nn", "")//
           .withResult("emp", "")//
           .withResult("nemp", "")//
           .withResult("likeMiddle", "")//
           .withResult("likeStartsWith", "")//
           .withResult("likeEndsWith", "")//
           .withResult("sw", "")//
           .withResult("ew", "")//
           .withResult("w", "")//
           .withResult("wo", "")//
           .withResult("lt", "")//
           .withResult("le", "")//
           .withResult("gt", "")//
           .withResult("ge", "")//
           .withResult("in", "")//
           .withResult("out", "")//
           .withResult("and", "")//
           .withResult("or", "")//
           .withResult("not", "")//
           .withResult("as", "")//
           .withResult("includes", "")//
           .withResult("distinct", "")//
           .withResult("count1", "")//
           .withResult("count2", "")//
           .withResult("count3", "")//
           .withResult("countAs", "")//
           .withResult("sum", "")//
           .withResult("sumAs", "")//
           .withResult("sumIf", "")//
           .withResult("min", "")//
           .withResult("max", "")//
           .withResult("groupCount", "")//
           .withResult("offset", "")//
           .withResult("limit", "")//
           .withResult("page", "")//
           .withResult("pageNum", "")//
           .withResult("after", "")//
           .withResult("sort", "")//
           .withResult("order", "")//
      ;
   }

   /**
    * Override me to customize integ tests
    * @param suite
    */

   protected void customizeIntegTestSuite(RqlValidationSuite suite)
   {

   }

}
