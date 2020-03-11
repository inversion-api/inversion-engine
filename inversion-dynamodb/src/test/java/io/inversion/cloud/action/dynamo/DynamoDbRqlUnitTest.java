package io.inversion.cloud.action.dynamo;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.Db;
import io.inversion.cloud.model.Index;
import io.inversion.cloud.model.Relationship;
import io.inversion.cloud.rql.AbstractRqlTest;
import io.inversion.cloud.rql.RqlValidationSuite;
import io.inversion.cloud.utils.Utils;

/**
 * Implements supported RQL test cases and adds extended cases to support
 * the verification of correct index selection.
 * 
 * <pre>
 * 
 * DynamoDb Table Indexes
 * 
 * | Property        | Index 
 * |-----------------|------------
 * | orderId         | p:hk, gs1:hk, gs3:sk        
 * | type            | p:sk, gs3:hk
 * | customerId      | gs2:hk
 * | employeeId      | gs1:hk 
 * | orderDate       | gs1:sk
 * | requiredDate    | ls3,gs2:sk
 * | shippedDate     |
 * | shipVia         |
 * | freight         |
 * | shipName        | ls2
 * | shipAddress     |
 * | shipCity        | ls1
 * | shipRegion      |
 * | shipPostalCode  |
 * | shipCountry     |
 *  
 *  
 * Additional Test Cases      
 *            
 * | Case | P:HK   | P:SK   | GS1:HK | GS1:SK | GS2:HK | GS2:SK | GS3:HK | GS3:SK | LS1    | LS2    | LS3    | FIELD-N  | COOSE             | Notes         
 * |------|--------|--------|--------|--------|--------|--------|--------|--------|--------|--------|--------|----------|-------------------|------------------------------|
 * |  A   |        |        |        |        |        |        |        |        |        |        |        |    =     | Scan              | eq(ShipPostalCode,30305)
 * |  B   |        |        |        |        |        |        |        |        |  =     |        |        |          | Scan              | eq(shipName,something)
 * |  C   |  =     |        |        |        |        |        |        |        |        |        |        |          | Query - PRIMARY   | eq(orderId, 12345)
 * |  D   |  =     |  =     |        |        |        |        |        |        |        |        |        |          | GetItem - PRIMARY | eq(orderId, 12345)&eq(type, 'ORDER')
 * |  E   |  =     |  >     |        |        |        |        |        |        |        |        |        |          | Query - PRIMARY   | eq(orderId, 12345)&gt(type, 'AAAAA')
 * |  F   |  =     |  >     |        |        |        |        |        |        |  >     |        |        |          | Query - PRIMARY   | eq(orderId, 12345)&gt(type, 'AAAAA')&gt(shipCity,Atlanta)
 * |  G   |  =     |  sw    |        |        |        |        |        |        |        |        |        |          | Query - PRIMARY   | eq(orderId, 12345)&sw(type, 'ORD')
 * |  H   |  =     |  sw    |        |        |        |        |        |        |  =     |        |        |          | Query - LS1       | eq(orderId, 12345)&sw(type, 'ORD')&eq(shipCity,Atlanta)
 * SHOULDNT THIS BE A GET???  |  I   |  =     |  sw    |  =     |   =    |        |        |        |        |        |        |        |          | Query - GS1       | eq(orderId, 12345)&sw(type, 'ORD')&eq(customerId,9999)&eq(orderDate,'2013-01-08')
 * |  J   |  =     |  sw    | =      | sw     |  =     |  =     |        |        |        |        |        |          | Query - GS2       |
 * |  K   |  gt    |  =     |        |        |        |        |        |        |        |        |        |          | Query - GS3       | gt(orderId, 12345)&eq(type, 'ORDER")
 * |  L   |  gt    |  sw    | =      |        |        |        |        |        |        |        |        |          | ????              |                               
 * |  M   |        |        | =      |        |  =     |        |        |        |        |        |        |          | Query - GS2       | eq(customerId,val)      
 * 
 * </pre>
 * @author wells
 *
 */
@TestInstance(Lifecycle.PER_CLASS)
public class DynamoDbRqlUnitTest extends AbstractRqlTest
{

   public DynamoDbRqlUnitTest()
   {
      super(DynamoDbQuery.class.getName(), "dynamo");
      urlPrefix = "northwind/dynamodb/";
   }

   @Override
   public void initializeDb()
   {
      Db db = getDb();
      if (db == null)
      {
         db = new DynamoDb().withName("bad_name_missing_env_props_on_purpose");
         setDb(db);
      }
   }

   protected void customizeUnitTestTables(RqlValidationSuite suite)
   {
      super.customizeUnitTestTables(suite);

      Collection orders = suite.getTable("orders");
      orders.withProperties("type", "S");

      Collection orderDetails = suite.getTable("orderDetails");
      Collection employees = suite.getTable("employees");
      Collection employeeOrderDetails = suite.getTable("employeeOrderDetails");

      for (Index index : orders.getIndexes())
         orders.removeIndex(index);

      for (Index index : orderDetails.getIndexes())
         orderDetails.removeIndex(index);

      for (Index index : employees.getIndexes())
         employees.removeIndex(index);

      for (Index index : employeeOrderDetails.getIndexes())
         employeeOrderDetails.removeIndex(index);

      for (Relationship r : orders.getRelationships())
         orders.removeRelationship(r);

      for (Relationship r : orderDetails.getRelationships())
         orderDetails.removeRelationship(r);

      for (Relationship r : employees.getRelationships())
         employees.removeRelationship(r);

      for (Relationship r : employeeOrderDetails.getRelationships())
         employeeOrderDetails.removeRelationship(r);

      orders.withIndex(DynamoDb.PRIMARY_INDEX_NAME, DynamoDb.PRIMARY_INDEX_TYPE, true, "orderId", "type");
      orders.withIndex("ls1", DynamoDb.LOCAL_SECONDARY_INDEX_TYPE, false, "orderId", "shipCity");
      orders.withIndex("ls2", DynamoDb.LOCAL_SECONDARY_INDEX_TYPE, false, "orderId", "shipName");
      orders.withIndex("ls3", DynamoDb.LOCAL_SECONDARY_INDEX_TYPE, false, "orderId", "requiredDate");
      orders.withIndex("gs1", DynamoDb.GLOBAL_SECONDARY_INDEX_TYPE, false, "employeeId", "orderDate");
      orders.withIndex("gs2", DynamoDb.GLOBAL_SECONDARY_INDEX_TYPE, false, "customerId", "requiredDate");
      orders.withIndex("gs3", DynamoDb.GLOBAL_SECONDARY_INDEX_TYPE, false, "type", "orderId");

      orderDetails.withIndex(DynamoDb.PRIMARY_INDEX_NAME, DynamoDb.PRIMARY_INDEX_TYPE, true, "orderId", "productId");
      orderDetails.getProperty("orderId").withPk(orders.getProperty("orderId"));

      employees.withIndex(DynamoDb.PRIMARY_INDEX_NAME, DynamoDb.PRIMARY_INDEX_TYPE, true, "employeeId", "type");
      employees.getProperty("reportsTo").withPk(employees.getProperty("employeeId"));
      employees.withIndex("fkIdx_Employees_reportsTo", "FOREIGN_KEY", false, "reportsTo");
      employees.withRelationship(new Relationship("reportsTo", Relationship.REL_ONE_TO_MANY, employees, employees, employees.getIndex("fkIdx_Employees_reportsTo"), null));
      employees.withRelationship(new Relationship("employees", Relationship.REL_MANY_TO_ONE, employees, employees, employees.getIndex("fkIdx_Employees_reportsTo"), null));

      employeeOrderDetails.withIndex(DynamoDb.PRIMARY_INDEX_NAME, DynamoDb.PRIMARY_INDEX_TYPE, true, "employeeId", "orderId", "productId");
      employeeOrderDetails.getProperty("employeeId").withPk(employees.getProperty("employeeId"));
      employeeOrderDetails.getProperty("orderId").withPk(orderDetails.getProperty("orderId"));
      employeeOrderDetails.getProperty("productId").withPk(orderDetails.getProperty("productId"));

      employeeOrderDetails.withIndex("FK_EOD_employeeId", "FOREIGN_KEY", false, "employeeId");
      employeeOrderDetails.withIndex("FK_EOD_orderdetails", "FOREIGN_KEY", false, "orderId", "productId");

      employees.withRelationship(new Relationship("orderdetails", Relationship.REL_MANY_TO_MANY, employees, orderDetails, employeeOrderDetails.getIndex("FK_EOD_employeeId"), employeeOrderDetails.getIndex("FK_EOD_orderdetails")));

      suite.withTables(orders, orderDetails, employees, employeeOrderDetails);

   }

   /**
    * The majority of these should be postgres/h2 compatible.  Mysql and MsSQL 
    * will probably have to customize most of these.
    */
   @Override
   protected void customizeUnitTestSuite(RqlValidationSuite suite)
   {
      super.customizeUnitTestSuite(suite);

      for (String testKey : (List<String>) new ArrayList(suite.getTests().keySet()))
      {
         String queryString = suite.getTests().get(testKey);

         if (queryString == null)
            continue;

         if (queryString.indexOf("type") < 0)
         {
            if (queryString.indexOf("?") < 0)
               queryString += "?";
            else
               queryString += "&";

            if (queryString.startsWith("orders"))
            {
               queryString += "type=ORDER";
               suite.withTest(testKey, queryString);
            }

         }
      }

      suite//
           .withResult("eq", "GetItemSpec:'Primary Index' key: [{orderID: 10248}, {type: ORDER}]")//
           .withResult("ne", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipCountry} valueMap={:val1=ORDER, :val2=France} filterExpression='(#var2 <> :val2)' keyConditionExpression='(#var1 = :val1)'")//
           .withResult("n", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipRegion, #var3=shipRegion} valueMap={:val1=ORDER, :val2=null} filterExpression='(attribute_not_exists(#var2) or (#var3 = :val2))' keyConditionExpression='(#var1 = :val1)'")//
           .withResult("nn", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipRegion, #var3=shipRegion} valueMap={:val1=ORDER, :val2=null} filterExpression='attribute_exists(#var2) and (#var3 <> :val2)' keyConditionExpression='(#var1 = :val1)'")//
           .withResult("emp", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipRegion, #var3=shipRegion} valueMap={:val1=ORDER, :val2=null} filterExpression='(attribute_not_exists(#var2) or (#var3 = :val2))' keyConditionExpression='(#var1 = :val1)'")//
           .withResult("nemp", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipRegion, #var3=shipRegion} valueMap={:val1=ORDER, :val2=null} filterExpression='attribute_exists(#var2) and (#var3 <> :val2)' keyConditionExpression='(#var1 = :val1)'")//
           .withResult("likeMiddle", "DynamoDb only supports a 'value*' or '*value*' wildcard formats which are equivalant to the 'sw' and 'w' operators.")//contains operator
           .withResult("likeStartsWith", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipCountry} valueMap={:val1=ORDER, :val2=Franc} filterExpression='begins_with(#var2,:val2)' keyConditionExpression='(#var1 = :val1)'")//startswith
           .withResult("likeEndsWith", "DynamoDb only supports a 'value*' or '*value*' wildcard formats which are equivalant to the 'sw' and 'w' operators.")//
           .withResult("sw", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipCountry} valueMap={:val1=ORDER, :val2=Franc} filterExpression='begins_with(#var2,:val2)' keyConditionExpression='(#var1 = :val1)'")//
           .withResult("ew", "UNSUPPORTED")//
           .withResult("w", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipCountry} valueMap={:val1=ORDER, :val2=ance} filterExpression='contains(#var2,:val2)' keyConditionExpression='(#var1 = :val1)'")//contains
           .withResult("wo", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipCountry} valueMap={:val1=ORDER, :val2=ance} filterExpression='(NOT contains(#var2,:val2))' keyConditionExpression='(#var1 = :val1)'")//not contains
           .withResult("lt", "QuerySpec:'gs3' nameMap={#var1=type, #var2=freight} valueMap={:val1=ORDER, :val2=10} filterExpression='(#var2 < :val2)' keyConditionExpression='(#var1 = :val1)'")//
           .withResult("le", "QuerySpec:'gs3' nameMap={#var1=type, #var2=freight} valueMap={:val1=ORDER, :val2=10} filterExpression='(#var2 <= :val2)' keyConditionExpression='(#var1 = :val1)'")//
           .withResult("gt", "QuerySpec:'gs3' nameMap={#var1=type, #var2=freight} valueMap={:val1=ORDER, :val2=3.67} filterExpression='(#var2 > :val2)' keyConditionExpression='(#var1 = :val1)'")//
           .withResult("ge", "QuerySpec:'gs3' nameMap={#var1=type, #var2=freight} valueMap={:val1=ORDER, :val2=3.67} filterExpression='(#var2 >= :val2)' keyConditionExpression='(#var1 = :val1)'")//
           .withResult("in", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipCity} valueMap={:val1=ORDER, :val2=Reims, :val3=Charleroi} filterExpression='(#var2 IN (:val2, :val3))' keyConditionExpression='(#var1 = :val1)'")//
           .withResult("out", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipCity} valueMap={:val1=ORDER, :val2=Reims, :val3=Charleroi} filterExpression='(NOT #var2 IN (:val2, :val3))' keyConditionExpression='(#var1 = :val1)'")//
           .withResult("and", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipCity, #var3=shipCountry} valueMap={:val1=ORDER, :val2=Lyon, :val3=France} filterExpression='(#var2 = :val2) and (#var3 = :val3)' keyConditionExpression='(#var1 = :val1)'")//
           .withResult("or", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipCity, #var3=shipCity} valueMap={:val1=ORDER, :val2=Reims, :val3=Charleroi} filterExpression='((#var2 = :val2) or (#var3 = :val3))' keyConditionExpression='(#var1 = :val1)'")//
           .withResult("not", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipCity, #var3=shipCity} valueMap={:val1=ORDER, :val2=Reims, :val3=Charleroi} filterExpression='(NOT ((#var2 = :val2) or (#var3 = :val3)))' keyConditionExpression='(#var1 = :val1)'")//
           .withResult("as", "UNSUPPORTED")//
           .withResult("includes", "QuerySpec:'gs3' nameMap={#var1=type} valueMap={:val1=ORDER} projectionExpression='shipCountry,shipCity' keyConditionExpression='(#var1 = :val1)'")//
           .withResult("distinct", "UNSUPPORTED")//
           .withResult("count1", "UNSUPPORTED")//
           .withResult("count2", "UNSUPPORTED")//
           .withResult("count3", "UNSUPPORTED")//
           .withResult("countAs", "UNSUPPORTED")//
           .withResult("sum", "UNSUPPORTED")//
           .withResult("sumAs", "UNSUPPORTED")//
           .withResult("sumIf", "UNSUPPORTED")//
           .withResult("min", "UNSUPPORTED")//
           .withResult("max", "UNSUPPORTED")//
           .withResult("groupCount", "UNSUPPORTED")//
           .withResult("offset", "UNSUPPORTED")//
           .withResult("limit", "QuerySpec:'gs3' maxResultSize=7 nameMap={#var1=type} valueMap={:val1=ORDER} keyConditionExpression='(#var1 = :val1)'")//
           .withResult("page", "UNSUPPORTED")//
           .withResult("pageNum", "UNSUPPORTED")//

           .withTest("after", "orders?type=ORDER&after(type,ORDER,orderId,10254)")//
           .withResult("after", "QuerySpec:'gs3' nameMap={#var1=type} valueMap={:val1=ORDER} exclusiveStartKey='[{type: ORDER}, {orderId: 10254}] keyConditionExpression='(#var1 = :val1)'")//

           .withTest("sort", "orders?eq(type,ORDER)&sort(-orderId)")//
           .withResult("sort", "QuerySpec:'gs3' nameMap={#var1=type} valueMap={:val1=ORDER} keyConditionExpression='(#var1 = :val1)' scanIndexForward=false")//

           .withTest("order", "orders?eq(customerId,12345)&order(-requiredDate)")//
           .withResult("order", "QuerySpec:'gs2' nameMap={#var1=customerId} valueMap={:val1=12345} keyConditionExpression='(#var1 = :val1)' scanIndexForward=false")//

           .withResult("onToManyExistsEq", "UNSUPPORTED")//
           .withResult("onToManyNotExistsNe", "UNSUPPORTED")//
           .withResult("manyToOneExistsEq", "UNSUPPORTED")//
           .withResult("manyToOneNotExistsNe", "UNSUPPORTED")//
           .withResult("manyTManyNotExistsNe", "UNSUPPORTED")//

           .withTest("A_scanWhenUnindexedFieldProvided", "orders?shipPostalCode=30305")//
           .withResult("A_scanWhenUnindexedFieldProvided", "ScanSpec nameMap={#var1=shipPostalCode} valueMap={:val1=30305} filterExpression='(#var1 = :val1)'")//

           .withTest("B_scanWhenOnlyLocalSecondaryProvided", "orders?eq(shipName,something)")//
           .withResult("B_scanWhenOnlyLocalSecondaryProvided", "ScanSpec nameMap={#var1=shipName} valueMap={:val1=something} filterExpression='(#var1 = :val1)'")//

           .withTest("C_queryPIWhenHashKeyProvided", "orders?eq(orderId, 12345)")//
           .withResult("C_queryPIWhenHashKeyProvided", "QuerySpec:'Primary Index' nameMap={#var1=orderId} valueMap={:val1=12345} keyConditionExpression='(#var1 = :val1)'")//

           .withTest("D_getWhenHashAndSortProvided", "orders?eq(orderId, 12345)&eq(type, 'ORDER')")//
           .withResult("D_getWhenHashAndSortProvided", "GetItemSpec:'Primary Index' key: [{orderId: 12345}, {type: ORDER}]")//

           .withTest("E_queryPi", "orders?eq(orderId, 12345)&gt(type, 'AAAAA')")//
           .withResult("E_queryPi", "QuerySpec:'Primary Index' nameMap={#var1=orderId, #var2=type} valueMap={:val1=12345, :val2=AAAAA} keyConditionExpression='(#var1 = :val1) and (#var2 > :val2)'")//

           .withTest("F_queryPi", "orders?eq(orderId, 12345)&gt(type, 'AAAAA')&gt(ShipCity,Atlanta)")//
           .withResult("F_queryPi", "QuerySpec:'Primary Index' nameMap={#var1=orderId, #var2=type, #var3=ShipCity} valueMap={:val1=12345, :val2=AAAAA, :val3=Atlanta} filterExpression='(#var3 > :val3)' keyConditionExpression='(#var1 = :val1) and (#var2 > :val2)'")

           .withTest("G_queryPi", "orders?eq(orderId, 12345)&sw(type, 'ORD')")//
           .withResult("G_queryPi", "QuerySpec:'Primary Index' nameMap={#var1=orderId, #var2=type} valueMap={:val1=12345, :val2=ORD} keyConditionExpression='(#var1 = :val1) and begins_with(#var2,:val2)'")//

           .withTest("H_queryLs1WhenHkEqAndLs1Eq", "orders?eq(orderId, 12345)&sw(type, 'ORD')&eq(ShipCity,Atlanta)")//
           .withResult("H_queryLs1WhenHkEqAndLs1Eq", "QuerySpec:'ls1' nameMap={#var1=orderId, #var2=ShipCity, #var3=type} valueMap={:val1=12345, :val2=Atlanta, :val3=ORD} filterExpression='begins_with(#var3,:val3)' keyConditionExpression='(#var1 = :val1) and (#var2 = :val2)'")//

           .withTest("I_queryGs1When", "orders?eq(orderId, 12345)&sw(type,ORD)&eq(employeeId,9999)&eq(orderDate,'2013-01-08')")//
           .withResult("I_queryGs1When", "QuerySpec:'gs1' nameMap={#var4=type, #var1=employeeId, #var2=orderDate, #var3=orderId} valueMap={:val1=9999, :val2=2013-01-08, :val3=12345, :val4=ORD} filterExpression='(#var3 = :val3) and begins_with(#var4,:val4)' keyConditionExpression='(#var1 = :val1) and (#var2 = :val2)'")//

           .withTest("K_queryGs3", "orders?gt(orderId, 12345)&eq(type,ORDER)")//
           .withResult("K_queryGs3", "QuerySpec:'gs3' nameMap={#var1=type, #var2=orderId} valueMap={:val1=ORDER, :val2=12345} keyConditionExpression='(#var1 = :val1) and (#var2 > :val2)'")//

           .withTest("M_queryGs2WhenGs2HkEq", "orders?eq(customerId,1234)")//
           .withResult("M_queryGs2WhenGs2HkEq", "QuerySpec:'gs2' nameMap={#var1=customerId} valueMap={:val1=1234} keyConditionExpression='(#var1 = :val1)'")//
      //.withTest("", "").withResult("",  "")//
      ;

   }

}
