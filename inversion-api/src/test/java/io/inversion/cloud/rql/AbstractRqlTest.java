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

import io.inversion.cloud.model.AbstractEngineTest;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.Db;
import io.inversion.cloud.model.Relationship;
import io.inversion.cloud.service.Engine;
import org.junit.jupiter.api.Test;

public abstract class AbstractRqlTest implements AbstractEngineTest
{
   protected String queryClass = null;
   protected String urlPrefix  = null;
   protected Engine engine     = null;
   protected String type       = null;
   protected Db     db         = null;

   public AbstractRqlTest(String queryClass, String dbType)
   {
      this.queryClass = queryClass;
      this.type = dbType;
   }


   @Test
   public void test_doSelect_unitTests() throws Exception
   {
      if (isIntegTest())
      {
         System.out.println("Skipping units tests...I have 'IntegTest' in my classname so I am skipping the unit tests expecting a diffent subclass without 'IntegTest' in the name is running those.");
         return;
      }

      RqlValidationSuite suite = new RqlValidationSuite(queryClass, db);
      customizeUnitTestTables(suite);
      customizeUnitTestSuite(suite);
      suite.runUnitTests();
   }

   @Test
   public void test_doSelect_integTests() throws Exception
   {
      if (!isIntegTest())
      {
         System.out.println("Skipping integ tests...subclasse me with 'IntegTest' in my classname to get me to run integ tests");
         return;
      }

      RqlValidationSuite suite = new RqlValidationSuite(queryClass, db);
      customizeIntegTestTables(suite);
      customizeIntegTestSuite(suite);
      suite.runIntegTests(engine, urlPrefix);
   }

   protected void customizeUnitTestTables(RqlValidationSuite suite)
   {
      Collection orders = new Collection("orders")//s
                                                  .withProperty("orderId", "VARCHAR")//
                                                  .withProperty("customerId", "INTEGER")//
                                                  .withProperty("employeeId", "INTEGER")//
                                                  .withProperty("orderDate", "DATETIME")//
                                                  .withProperty("requiredDate", "DATETIME")//
                                                  .withProperty("shippedDate", "DATETIME")//
                                                  .withProperty("shipVia", "INTEGER")//
                                                  .withProperty("freight", "DECIMAL")//
                                                  .withProperty("shipName", "VARCHAR")//
                                                  .withProperty("shipAddress", "VARCHAR")//
                                                  .withProperty("shipCity", "VARCHAR")//
                                                  .withProperty("shipRegion", "VARCHAR")//
                                                  .withProperty("shipPostalCode", "VARCHAR")//
                                                  .withProperty("shipCountry", "VARCHAR")//
                                                  .withIndex("PK_Orders", "primary", true, "orderId");

      Collection orderDetails = new Collection("orderDetails").withProperties("employeeId", "INTEGER")//
                                                              .withProperty("orderId", "INTEGER")//
                                                              .withProperty("productId", "INTEGER")//
                                                              .withProperty("quantity", "INTEGER")//
                                                              .withIndex("PK_orderDetails", "primary", true, "orderId", "productId");

      orderDetails.getProperty("orderId").withPk(orders.getProperty("orderId"));

      Collection employees = new Collection("employees").withProperty("employeeId", "INTEGER")//
                                                        .withProperty("firstName", "VARCHAR")//
                                                        .withProperty("lastName", "VARCHAR")//
                                                        .withProperty("reportsTo", "INTEGER")//
                                                        .withIndex("PK_Employees", "primary", true, "employeeId");

      employees.getProperty("reportsTo").withPk(employees.getProperty("employeeId"));
      employees.withIndex("fkIdx_Employees_reportsTo", "FOREIGN_KEY", false, "reportsTo");

      employees.withRelationship(new Relationship("reportsTo", Relationship.REL_ONE_TO_MANY, employees, employees, employees.getIndex("fkIdx_Employees_reportsTo"), null));
      employees.withRelationship(new Relationship("employees", Relationship.REL_MANY_TO_ONE, employees, employees, employees.getIndex("fkIdx_Employees_reportsTo"), null));

      Collection employeeOrderDetails = new Collection("employeeOrderDetails")//
                                                                              .withProperty("employeeId", "INTEGER")//
                                                                              .withProperty("orderId", "INTEGER")//
                                                                              .withProperty("productId", "INTEGER")//
                                                                              .withIndex("PK_EmployeeOrderDetails", "primary", true, "employeeId", "orderId", "productId");

      employeeOrderDetails.getProperty("employeeId").withPk(employees.getProperty("employeeId"));
      employeeOrderDetails.getProperty("orderId").withPk(orderDetails.getProperty("orderId"));
      employeeOrderDetails.getProperty("productId").withPk(orderDetails.getProperty("productId"));

      employeeOrderDetails.withIndex("FK_EOD_employeeId", "FOREIGN_KEY", false, "employeeId");
      employeeOrderDetails.withIndex("FK_EOD_orderdetails", "FOREIGN_KEY", false, "orderId", "productId");

      employees.withRelationship(new Relationship("orderdetails", Relationship.REL_MANY_TO_MANY, employees, orderDetails, employeeOrderDetails.getIndex("FK_EOD_employeeId"), employeeOrderDetails.getIndex("FK_EOD_orderdetails")));

      suite.withTables(orders, orderDetails, employees, employeeOrderDetails);
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
           .withResult("onToManyExistsEq", "")//
           .withResult("onToManyNotExistsNe", "")//
           .withResult("manyToOneExistsEq", "")//
           .withResult("manyToOneNotExistsNe", "")//
           .withResult("manyTManyNotExistsNe", "")//
           .withResult("eqNonexistantColumn",  "");
      ;
   }

   protected void customizeIntegTestTables(RqlValidationSuite suite)
   {
      customizeUnitTestTables(suite);
   }

   /**
    * Override me to customize integ tests
    * @param suite
    */

   protected void customizeIntegTestSuite(RqlValidationSuite suite)
   {
      customizeUnitTestSuite(suite);
   }

   public Engine getEngine()
   {
      return engine;
   }

   public void setEngine(Engine engine)
   {
      this.engine = engine;
   }

   public String getType()
   {
      return type;
   }

   public void setType(String type)
   {
      this.type = type;
   }

   public Db getDb()
   {
      return db;
   }

   public void setDb(Db db)
   {
      this.db = db;
   }

}
