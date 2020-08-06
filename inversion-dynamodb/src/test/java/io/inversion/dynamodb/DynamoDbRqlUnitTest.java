package io.inversion.dynamodb;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.inversion.rql.AbstractRqlTest;

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
 * |  I   |  =     |  sw    |  =     |   =    |        |        |        |        |        |        |        |          | Query - GS1       | eq(orderId, 12345)&sw(type, 'ORD')&eq(customerId,9999)&eq(orderDate,'2013-01-08')
 * |  J   |  =     |  sw    | =      | sw     |  =     |  =     |        |        |        |        |        |          | Query - GS2       |
 * |  K   |  gt    |  =     |        |        |        |        |        |        |        |        |        |          | Query - GS3       | gt(orderId, 12345)&eq(type, 'ORDER")
 * |  L   |  gt    |  sw    | =      |        |        |        |        |        |        |        |        |          | ????              |
 * |  M   |        |        | =      |        |  =     |        |        |        |        |        |        |          | Query - GS2       | eq(customerId,val)
 *
 * </pre>
 */
@TestInstance(Lifecycle.PER_CLASS)
public class DynamoDbRqlUnitTest extends AbstractRqlTest implements AbstractDynamoTest {

    public DynamoDbRqlUnitTest() {
        super("northwind/dynamodb/", "dynamodb");

        for (String testKey : testRequests.keySet()) {
            String queryString = testRequests.get(testKey);

            if (queryString == null)
                continue;

            if (queryString.indexOf("type") < 0) {
                if (queryString.indexOf("?") < 0)
                    queryString += "?";
                else
                    queryString += "&";

                if (queryString.startsWith("orders")) {
                    queryString += "type=ORDER";
                    withTestRequest(testKey, queryString);
                }
            }
        }

        withExpectedResult("eq", "GetItemSpec:'Primary Index' key: [{orderID: 10248}, {type: ORDER}]");
        withExpectedResult("ne", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipCountry} valueMap={:val1=ORDER, :val2=France} filterExpression='(#var2 <> :val2)' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("n", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipRegion, #var3=shipRegion} valueMap={:val1=ORDER, :val2=null} filterExpression='(attribute_not_exists(#var2) or (#var3 = :val2))' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("nn", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipRegion, #var3=shipRegion} valueMap={:val1=ORDER, :val2=null} filterExpression='attribute_exists(#var2) and (#var3 <> :val2)' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("emp", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipRegion, #var3=shipRegion} valueMap={:val1=ORDER, :val2=null} filterExpression='(attribute_not_exists(#var2) or (#var3 = :val2))' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("nemp", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipRegion, #var3=shipRegion} valueMap={:val1=ORDER, :val2=null} filterExpression='attribute_exists(#var2) and (#var3 <> :val2)' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("likeMiddle", "400 Bad Request - DynamoDb only supports a 'value*' or '*value*' wildcard formats which are equivalant to the 'sw' and 'w' operators.");//contains operator
        withExpectedResult("likeStartsWith", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipCountry} valueMap={:val1=ORDER, :val2=Franc} filterExpression='begins_with(#var2,:val2)' keyConditionExpression='(#var1 = :val1)'");//startswith
        withExpectedResult("likeEndsWith", "400 Bad Request - DynamoDb only supports a 'value*' or '*value*' wildcard formats which are equivalant to the 'sw' and 'w' operators.");
        withExpectedResult("sw", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipCountry} valueMap={:val1=ORDER, :val2=Franc} filterExpression='begins_with(#var2,:val2)' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("ew", "UNSUPPORTED");
        withExpectedResult("w", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipCountry} valueMap={:val1=ORDER, :val2=ance} filterExpression='contains(#var2,:val2)' keyConditionExpression='(#var1 = :val1)'");//contains
        withExpectedResult("wo", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipCountry} valueMap={:val1=ORDER, :val2=ance} filterExpression='(NOT contains(#var2,:val2))' keyConditionExpression='(#var1 = :val1)'");//not contains
        withExpectedResult("lt", "QuerySpec:'gs3' nameMap={#var1=type, #var2=freight} valueMap={:val1=ORDER, :val2=10} filterExpression='(#var2 < :val2)' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("le", "QuerySpec:'gs3' nameMap={#var1=type, #var2=freight} valueMap={:val1=ORDER, :val2=10} filterExpression='(#var2 <= :val2)' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("gt", "QuerySpec:'gs3' nameMap={#var1=type, #var2=freight} valueMap={:val1=ORDER, :val2=3.67} filterExpression='(#var2 > :val2)' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("ge", "QuerySpec:'gs3' nameMap={#var1=type, #var2=freight} valueMap={:val1=ORDER, :val2=3.67} filterExpression='(#var2 >= :val2)' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("in", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipCity} valueMap={:val1=ORDER, :val2=Reims, :val3=Charleroi} filterExpression='(#var2 IN (:val2, :val3))' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("out", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipCity} valueMap={:val1=ORDER, :val2=Reims, :val3=Charleroi} filterExpression='(NOT #var2 IN (:val2, :val3))' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("and", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipCity, #var3=shipCountry} valueMap={:val1=ORDER, :val2=Lyon, :val3=France} filterExpression='(#var2 = :val2) and (#var3 = :val3)' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("or", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipCity, #var3=shipCity} valueMap={:val1=ORDER, :val2=Reims, :val3=Charleroi} filterExpression='((#var2 = :val2) or (#var3 = :val3))' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("not", "QuerySpec:'gs3' nameMap={#var1=type, #var2=shipCity, #var3=shipCity} valueMap={:val1=ORDER, :val2=Reims, :val3=Charleroi} filterExpression='(NOT ((#var2 = :val2) or (#var3 = :val3)))' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("as", "UNSUPPORTED");
        withExpectedResult("includes", "QuerySpec:'gs3' nameMap={#var1=type} valueMap={:val1=ORDER} projectionExpression='shipCountry,shipCity,orderId,type' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("distinct", "UNSUPPORTED");
        withExpectedResult("count1", "UNSUPPORTED");
        withExpectedResult("count2", "UNSUPPORTED");
        withExpectedResult("count3", "UNSUPPORTED");
        withExpectedResult("countAs", "UNSUPPORTED");
        withExpectedResult("sum", "UNSUPPORTED");
        withExpectedResult("sumAs", "UNSUPPORTED");
        withExpectedResult("sumIf", "UNSUPPORTED");
        withExpectedResult("min", "UNSUPPORTED");
        withExpectedResult("max", "UNSUPPORTED");
        withExpectedResult("groupCount", "UNSUPPORTED");
        withExpectedResult("offset", "UNSUPPORTED");
        withExpectedResult("limit", "QuerySpec:'gs3' maxResultSize=7 nameMap={#var1=type} valueMap={:val1=ORDER} keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("page", "UNSUPPORTED");
        withExpectedResult("pageNum", "UNSUPPORTED");

        withTestRequest("after", "orders?type=ORDER&after(type,ORDER,orderId,10254)");
        withExpectedResult("after", "QuerySpec:'gs3' nameMap={#var1=type} valueMap={:val1=ORDER} exclusiveStartKey='[{type: ORDER}, {orderId: 10254}] keyConditionExpression='(#var1 = :val1)'");

        withTestRequest("sort", "orders?eq(type,ORDER)&sort(-orderId)");
        withExpectedResult("sort", "QuerySpec:'gs3' nameMap={#var1=type} valueMap={:val1=ORDER} keyConditionExpression='(#var1 = :val1)' scanIndexForward=false");

        withTestRequest("order", "orders?eq(customerId,12345)&order(-requiredDate)");
        withExpectedResult("order", "QuerySpec:'gs2' nameMap={#var1=customerId} valueMap={:val1=12345} keyConditionExpression='(#var1 = :val1)' scanIndexForward=false");

        withExpectedResult("onToManyExistsEq", "UNSUPPORTED");
        withExpectedResult("onToManyNotExistsNe", "UNSUPPORTED");
        withExpectedResult("manyToOneExistsEq", "UNSUPPORTED");
        withExpectedResult("manyToOneNotExistsNe", "UNSUPPORTED");
        withExpectedResult("manyTManyNotExistsNe", "UNSUPPORTED");

        withExpectedResult("eqNonexistantColumn", "QuerySpec:'gs3' nameMap={#var1=type, #var2=orderId, #var3=nonexistantColumn} valueMap={:val1=ORDER, :val2=1000, :val3=12} filterExpression='(#var3 = :val3)' keyConditionExpression='(#var1 = :val1) and (#var2 >= :val2)'");

        withTestRequest("A_scanWhenUnindexedFieldProvided", "orders?shipPostalCode=30305");
        withExpectedResult("A_scanWhenUnindexedFieldProvided", "ScanSpec nameMap={#var1=shipPostalCode} valueMap={:val1=30305} filterExpression='(#var1 = :val1)'");

        withTestRequest("B_scanWhenOnlyLocalSecondaryProvided", "orders?eq(shipName,something)");
        withExpectedResult("B_scanWhenOnlyLocalSecondaryProvided", "ScanSpec nameMap={#var1=shipName} valueMap={:val1=something} filterExpression='(#var1 = :val1)'");

        withTestRequest("C_queryPIWhenHashKeyProvided", "orders?eq(orderId, 12345)");
        withExpectedResult("C_queryPIWhenHashKeyProvided", "QuerySpec:'Primary Index' nameMap={#var1=orderId} valueMap={:val1=12345} keyConditionExpression='(#var1 = :val1)'");

        withTestRequest("D_getWhenHashAndSortProvided", "orders?eq(orderId, 12345)&eq(type, 'ORDER')");
        withExpectedResult("D_getWhenHashAndSortProvided", "GetItemSpec:'Primary Index' key: [{orderId: 12345}, {type: ORDER}]");

        withTestRequest("E_queryPi", "orders?eq(orderId, 12345)&gt(type, 'AAAAA')");
        withExpectedResult("E_queryPi", "QuerySpec:'Primary Index' nameMap={#var1=orderId, #var2=type} valueMap={:val1=12345, :val2=AAAAA} keyConditionExpression='(#var1 = :val1) and (#var2 > :val2)'");

        withTestRequest("F_queryPi", "orders?eq(orderId, 12345)&gt(type, 'AAAAA')&gt(ShipCity,Atlanta)");
        withExpectedResult("F_queryPi", "QuerySpec:'Primary Index' nameMap={#var1=orderId, #var2=type, #var3=ShipCity} valueMap={:val1=12345, :val2=AAAAA, :val3=Atlanta} filterExpression='(#var3 > :val3)' keyConditionExpression='(#var1 = :val1) and (#var2 > :val2)'");

        withTestRequest("G_queryPi", "orders?eq(orderId, 12345)&sw(type, 'ORD')");
        withExpectedResult("G_queryPi", "QuerySpec:'Primary Index' nameMap={#var1=orderId, #var2=type} valueMap={:val1=12345, :val2=ORD} keyConditionExpression='(#var1 = :val1) and begins_with(#var2,:val2)'");

        withTestRequest("H_queryLs1WhenHkEqAndLs1Eq", "orders?eq(orderId, 12345)&sw(type, 'ORD')&eq(ShipCity,Atlanta)");
        withExpectedResult("H_queryLs1WhenHkEqAndLs1Eq", "QuerySpec:'ls1' nameMap={#var1=orderId, #var2=ShipCity, #var3=type} valueMap={:val1=12345, :val2=Atlanta, :val3=ORD} filterExpression='begins_with(#var3,:val3)' keyConditionExpression='(#var1 = :val1) and (#var2 = :val2)'");

        withTestRequest("I_queryGs1When", "orders?eq(orderId, 12345)&sw(type,ORD)&eq(employeeId,9999)&eq(orderDate,'2013-01-08')");
        withExpectedResult("I_queryGs1When", "QuerySpec:'gs1' nameMap={#var4=type, #var1=employeeId, #var2=orderDate, #var3=orderId} valueMap={:val1=9999, :val2=2013-01-08, :val3=12345, :val4=ORD} filterExpression='(#var3 = :val3) and begins_with(#var4,:val4)' keyConditionExpression='(#var1 = :val1) and (#var2 = :val2)'");

        withTestRequest("K_queryGs3", "orders?gt(orderId, 12345)&eq(type,ORDER)");
        withExpectedResult("K_queryGs3", "QuerySpec:'gs3' nameMap={#var1=type, #var2=orderId} valueMap={:val1=ORDER, :val2=12345} keyConditionExpression='(#var1 = :val1) and (#var2 > :val2)'");

        withTestRequest("M_queryGs2WhenGs2HkEq", "orders?eq(customerId,1234)");
        withExpectedResult("M_queryGs2WhenGs2HkEq", "QuerySpec:'gs2' nameMap={#var1=customerId} valueMap={:val1=1234} keyConditionExpression='(#var1 = :val1)'");
    }

    //    @Override
    //    public Db buildDb() {
    //        Db db = new DynamoDb().withName("bad_name_missing_env_props_on_purpose");
    //        if (isIntegTest()) {
    //            db = DynamoDbFactory.buildNorthwindDynamoDb();
    //        } else {
    //            configureDefaultModel(db);
    //
    //            Collection orders = db.getCollectionByTableName("orders");
    //            orders.withProperty("type", "S");
    //
    //            Collection orderDetails = db.getCollectionByTableName("orderDetails");
    //            Collection employees = db.getCollectionByTableName("employees");
    //            Collection employeeOrderDetails = db.getCollectionByTableName("employeeOrderDetails");
    //
    //            for (Index index : orders.getIndexes())
    //                orders.removeIndex(index);
    //
    //            for (Index index : orderDetails.getIndexes())
    //                orderDetails.removeIndex(index);
    //
    //            for (Index index : employees.getIndexes())
    //                employees.removeIndex(index);
    //
    //            for (Index index : employeeOrderDetails.getIndexes())
    //                employeeOrderDetails.removeIndex(index);
    //
    //            for (Relationship r : orders.getRelationships())
    //                orders.removeRelationship(r);
    //
    //            for (Relationship r : orderDetails.getRelationships())
    //                orderDetails.removeRelationship(r);
    //
    //            for (Relationship r : employees.getRelationships())
    //                employees.removeRelationship(r);
    //
    //            for (Relationship r : employeeOrderDetails.getRelationships())
    //                employeeOrderDetails.removeRelationship(r);
    //
    //            orders.withIndex(DynamoDb.PRIMARY_INDEX_NAME, DynamoDb.PRIMARY_INDEX_TYPE, true, "orderId", "type");
    //            orders.withIndex("ls1", DynamoDb.LOCAL_SECONDARY_INDEX_TYPE, false, "orderId", "shipCity");
    //            orders.withIndex("ls2", DynamoDb.LOCAL_SECONDARY_INDEX_TYPE, false, "orderId", "shipName");
    //            orders.withIndex("ls3", DynamoDb.LOCAL_SECONDARY_INDEX_TYPE, false, "orderId", "requiredDate");
    //            orders.withIndex("gs1", DynamoDb.GLOBAL_SECONDARY_INDEX_TYPE, false, "employeeId", "orderDate");
    //            orders.withIndex("gs2", DynamoDb.GLOBAL_SECONDARY_INDEX_TYPE, false, "customerId", "requiredDate");
    //            orders.withIndex("gs3", DynamoDb.GLOBAL_SECONDARY_INDEX_TYPE, false, "type", "orderId");
    //
    //            orderDetails.withIndex(DynamoDb.PRIMARY_INDEX_NAME, DynamoDb.PRIMARY_INDEX_TYPE, true, "orderId", "productId");
    //            orderDetails.getProperty("orderId").withPk(orders.getProperty("orderId"));
    //
    //            employees.withProperty("type", "S");
    //            employees.withIndex(DynamoDb.PRIMARY_INDEX_NAME, DynamoDb.PRIMARY_INDEX_TYPE, true, "employeeId", "type");
    //            employees.getProperty("reportsTo").withPk(employees.getProperty("employeeId"));
    //            employees.withIndex("fkIdx_Employees_reportsTo", "FOREIGN_KEY", false, "reportsTo");
    //            employees.withRelationship(new Relationship("reportsTo", Relationship.REL_MANY_TO_ONE, employees, employees, employees.getIndex("fkIdx_Employees_reportsTo"), null));
    //            employees.withRelationship(new Relationship("employees", Relationship.REL_ONE_TO_MANY, employees, employees, employees.getIndex("fkIdx_Employees_reportsTo"), null));
    //
    //            employeeOrderDetails.withIndex(DynamoDb.PRIMARY_INDEX_NAME, DynamoDb.PRIMARY_INDEX_TYPE, true, "employeeId", "orderId", "productId");
    //            employeeOrderDetails.getProperty("employeeId").withPk(employees.getProperty("employeeId"));
    //            employeeOrderDetails.getProperty("orderId").withPk(orderDetails.getProperty("orderId"));
    //            employeeOrderDetails.getProperty("productId").withPk(orderDetails.getProperty("productId"));
    //
    //            employeeOrderDetails.withIndex("FK_EOD_employeeId", "FOREIGN_KEY", false, "employeeId");
    //            employeeOrderDetails.withIndex("FK_EOD_orderdetails", "FOREIGN_KEY", false, "orderId", "productId");
    //
    //            employees.withRelationship(new Relationship("orderdetails", Relationship.REL_MANY_TO_MANY, employees, orderDetails, employeeOrderDetails.getIndex("FK_EOD_employeeId"), employeeOrderDetails.getIndex("FK_EOD_orderdetails")));
    //
    //            db.withCollections(orders, orderDetails, employees, employeeOrderDetails);
    //        }
    //
    //        return db;
    //    }

}
