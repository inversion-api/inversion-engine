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
package io.inversion.dynamodb;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class DynamoDbRqlIntegTest extends DynamoDbRqlUnitTest {

    public DynamoDbRqlIntegTest() throws Exception {
        super();

        withExpectedResult("eq", "GetItemSpec:'Primary Index' key: [{hk: 10248}, {sk: ORDER}]");
        withExpectedResult("ne", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=shipCountry} valueMap={:val1=ORDER, :val2=France} filterExpression='(#var2 <> :val2)' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("n", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=shipRegion, #var3=shipRegion} valueMap={:val1=ORDER, :val2=null} filterExpression='(attribute_not_exists(#var2) or (#var3 = :val2))' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("nn", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=shipRegion, #var3=shipRegion} valueMap={:val1=ORDER, :val2=null} filterExpression='attribute_exists(#var2) and (#var3 <> :val2)' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("emp", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=shipRegion, #var3=shipRegion} valueMap={:val1=ORDER, :val2=null} filterExpression='(attribute_not_exists(#var2) or (#var3 = :val2))' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("nemp", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=shipRegion, #var3=shipRegion} valueMap={:val1=ORDER, :val2=null} filterExpression='attribute_exists(#var2) and (#var3 <> :val2)' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("likeMiddle", "UNSUPPORTED");
        withExpectedResult("likeStartsWith", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=shipCountry} valueMap={:val1=ORDER, :val2=Franc} filterExpression='begins_with(#var2,:val2)' keyConditionExpression='(#var1 = :val1)'");//startswith
        withExpectedResult("likeEndsWith", "UNSUPPORTED");
        withExpectedResult("sw", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=shipCountry} valueMap={:val1=ORDER, :val2=Franc} filterExpression='begins_with(#var2,:val2)' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("ew", "UNSUPPORTED");
        withExpectedResult("w", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=shipCountry} valueMap={:val1=ORDER, :val2=ance} filterExpression='contains(#var2,:val2)' keyConditionExpression='(#var1 = :val1)'");//contains
        withExpectedResult("wo", "DynamoDb: QuerySpec:'gs3' nameMap={#var1=sk, #var2=shipCountry} valueMap={:val1=ORDER, :val2=ance} filterExpression='(NOT contains(#var2,:val2))' keyConditionExpression='(#var1 = :val1)'");//not contains
        withExpectedResult("lt", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=freight} valueMap={:val1=ORDER, :val2=10} filterExpression='(#var2 < :val2)' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("le", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=freight} valueMap={:val1=ORDER, :val2=10} filterExpression='(#var2 <= :val2)' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("gt", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=freight} valueMap={:val1=ORDER, :val2=3.67} filterExpression='(#var2 > :val2)' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("ge", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=freight} valueMap={:val1=ORDER, :val2=3.67} filterExpression='(#var2 >= :val2)' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("in", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=ls1} valueMap={:val1=ORDER, :val2=Reims, :val3=Charleroi} filterExpression='(#var2 IN (:val2, :val3))' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("out", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=ls1} valueMap={:val1=ORDER, :val2=Reims, :val3=Charleroi} filterExpression='(NOT #var2 IN (:val2, :val3))' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("and", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=ls1, #var3=shipCountry} valueMap={:val1=ORDER, :val2=Lyon, :val3=France} filterExpression='(#var2 = :val2) and (#var3 = :val3)' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("or", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=ls1, #var3=ls1} valueMap={:val1=ORDER, :val2=Reims, :val3=Charleroi} filterExpression='((#var2 = :val2) or (#var3 = :val3))' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("not", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=ls1, #var3=ls1} valueMap={:val1=ORDER, :val2=Reims, :val3=Charleroi} filterExpression='(NOT ((#var2 = :val2) or (#var3 = :val3)))' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("as", "UNSUPPORTED");
        withExpectedResult("includes", "QuerySpec:'gs3' nameMap={#var1=sk} valueMap={:val1=ORDER} projectionExpression='shipCountry,ls1,hk,sk' keyConditionExpression='(#var1 = :val1)'");
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
        withExpectedResult("limit", "DynamoDb: QuerySpec:'gs3' maxResultSize=7 nameMap={#var1=sk} valueMap={:val1=ORDER} keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("page", "UNSUPPORTED");
        withExpectedResult("pageNum", "UNSUPPORTED");

        withExpectedResult("after", "QuerySpec:'gs3' nameMap={#var1=sk} valueMap={:val1=ORDER} exclusiveStartKey='[{sk: ORDER}, {hk: 10254}] keyConditionExpression='(#var1 = :val1)'");

        withExpectedResult("sort", "QuerySpec:'gs3' nameMap={#var1=sk} valueMap={:val1=ORDER} keyConditionExpression='(#var1 = :val1)' scanIndexForward=false");

        withExpectedResult("onToManyExistsEq", "UNSUPPORTED");
        withExpectedResult("onToManyNotExistsNe", "UNSUPPORTED");
        withExpectedResult("manyToOneExistsEq", "UNSUPPORTED");
        withExpectedResult("manyToOneNotExistsNe", "UNSUPPORTED");
        withExpectedResult("manyTManyNotExistsNe", "UNSUPPORTED");

        withExpectedResult("eqNonexistantColumn", "DynamoDb: QuerySpec:'gs3' nameMap={#var1=sk, #var2=hk, #var3=nonexistantColumn} valueMap={:val1=ORDER, :val2=1000, :val3=12} filterExpression='(#var3 = :val3)' keyConditionExpression='(#var1 = :val1) and (#var2 >= :val2)'");

        withExpectedResult("D_getWhenHashAndSortProvided", "GetItemSpec:'Primary Index' key: [{hk: 12345}, {sk: ORDER}]");
        withExpectedResult("E_queryPi", "QuerySpec:'Primary Index' nameMap={#var1=hk, #var2=sk} valueMap={:val1=12345, :val2=AAAAA} keyConditionExpression='(#var1 = :val1) and (#var2 > :val2)'");
        withExpectedResult("F_queryPi", "QuerySpec:'Primary Index' nameMap={#var1=hk, #var2=sk, #var3=ls1} valueMap={:val1=12345, :val2=AAAAA, :val3=Atlanta} filterExpression='(#var3 > :val3)' keyConditionExpression='(#var1 = :val1) and (#var2 > :val2)'");
        withExpectedResult("G_queryPi", "QuerySpec:'Primary Index' nameMap={#var1=hk, #var2=sk} valueMap={:val1=12345, :val2=ORD} keyConditionExpression='(#var1 = :val1) and begins_with(#var2,:val2)'");
        withExpectedResult("H_queryLs1WhenHkEqAndLs1Eq", "QuerySpec:'ls1' nameMap={#var1=hk, #var2=ls1, #var3=sk} valueMap={:val1=12345, :val2=Atlanta, :val3=ORD} filterExpression='begins_with(#var3,:val3)' keyConditionExpression='(#var1 = :val1) and (#var2 = :val2)'");
        withExpectedResult("I_queryGs1When", "QuerySpec:'gs1' nameMap={#var4=sk, #var1=gs1hk, #var2=gs1sk, #var3=hk} valueMap={:val1=9999, :val2=2013-01-08, :val3=12345, :val4=ORD} filterExpression='(#var3 = :val3) and begins_with(#var4,:val4)' keyConditionExpression='(#var1 = :val1) and (#var2 = :val2)'");
        withExpectedResult("K_queryGs3", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=hk} valueMap={:val1=ORDER, :val2=12345} keyConditionExpression='(#var1 = :val1) and (#var2 > :val2)'");
        withExpectedResult("M_queryGs2WhenGs2HkEq", "QuerySpec:'gs2' nameMap={#var1=gs2hk} valueMap={:val1=1234} keyConditionExpression='(#var1 = :val1)'");

        //-- WARNING WARNING these tests are passing but don't generate the same queries as their unit tests
        //-- because the integ test version always provides the "type" parameter on queries.  The unit test
        //-- version tests the actual scenario described
        withExpectedResult("order", "QuerySpec:'gs2' nameMap={#var1=gs2hk, #var2=sk} valueMap={:val1=12345, :val2=ORDER} filterExpression='(#var2 = :val2)' keyConditionExpression='(#var1 = :val1)' scanIndexForward=false");
        withExpectedResult("A_scanWhenUnindexedFieldProvided", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=shipPostalCode} valueMap={:val1=ORDER, :val2=30305} filterExpression='(#var2 = :val2)' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("B_scanWhenOnlyLocalSecondaryProvided", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=ls2} valueMap={:val1=ORDER, :val2=something} filterExpression='(#var2 = :val2)' keyConditionExpression='(#var1 = :val1)'");
        withExpectedResult("C_queryPIWhenHashKeyProvided", "DynamoDb: GetItemSpec:'Primary Index' key: [{hk: 12345}, {sk: ORDER}]");
        withExpectedResult("M_queryGs2WhenGs2HkEq", "DynamoDb: QuerySpec:'gs3' nameMap={#var1=sk, #var2=gs2hk} valueMap={:val1=ORDER, :val2=1234} filterExpression='(#var2 = :val2)' keyConditionExpression='(#var1 = :val1)'");

    }

    //    @Override
    //    public void initializeDb() {
    //        Db db = getDb();
    //        if (db == null) {
    //            try {
    //                db = DynamoDbFactory.buildNorthwindDynamoDb();
    //            } catch (Exception e) {
    //                Utils.rethrow(e);
    //            }
    //            setDb(db);
    //        }
    //    }

    //    protected void customizeIntegTestSuite(RqlValidationSuite suite) {
    //        super.customizeIntegTestSuite(suite);
    //
    //        suite//
    //                withExpectedResult("eq", "GetItemSpec:'Primary Index' key: [{hk: 10248}, {sk: ORDER}]");
    //                withExpectedResult("ne", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=shipCountry} valueMap={:val1=ORDER, :val2=France} filterExpression='(#var2 <> :val2)' keyConditionExpression='(#var1 = :val1)'");
    //                withExpectedResult("n", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=shipRegion, #var3=shipRegion} valueMap={:val1=ORDER, :val2=null} filterExpression='(attribute_not_exists(#var2) or (#var3 = :val2))' keyConditionExpression='(#var1 = :val1)'");
    //                withExpectedResult("nn", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=shipRegion, #var3=shipRegion} valueMap={:val1=ORDER, :val2=null} filterExpression='attribute_exists(#var2) and (#var3 <> :val2)' keyConditionExpression='(#var1 = :val1)'");
    //                withExpectedResult("emp", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=shipRegion, #var3=shipRegion} valueMap={:val1=ORDER, :val2=null} filterExpression='(attribute_not_exists(#var2) or (#var3 = :val2))' keyConditionExpression='(#var1 = :val1)'");
    //                withExpectedResult("nemp", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=shipRegion, #var3=shipRegion} valueMap={:val1=ORDER, :val2=null} filterExpression='attribute_exists(#var2) and (#var3 <> :val2)' keyConditionExpression='(#var1 = :val1)'");
    //                withExpectedResult("likeMiddle", "UNSUPPORTED")
    //                withExpectedResult("likeStartsWith", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=shipCountry} valueMap={:val1=ORDER, :val2=Franc} filterExpression='begins_with(#var2,:val2)' keyConditionExpression='(#var1 = :val1)'");startswith
    //                withExpectedResult("likeEndsWith", "UNSUPPORTED");
    //                withExpectedResult("sw", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=shipCountry} valueMap={:val1=ORDER, :val2=Franc} filterExpression='begins_with(#var2,:val2)' keyConditionExpression='(#var1 = :val1)'");
    //                withExpectedResult("ew", "UNSUPPORTED");
    //                withExpectedResult("w", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=shipCountry} valueMap={:val1=ORDER, :val2=ance} filterExpression='contains(#var2,:val2)' keyConditionExpression='(#var1 = :val1)'");contains
    //                withExpectedResult("wo", "DynamoDb: QuerySpec:'gs3' nameMap={#var1=sk, #var2=shipCountry} valueMap={:val1=ORDER, :val2=ance} filterExpression='(NOT contains(#var2,:val2))' keyConditionExpression='(#var1 = :val1)'");not contains
    //                withExpectedResult("lt", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=freight} valueMap={:val1=ORDER, :val2=10} filterExpression='(#var2 < :val2)' keyConditionExpression='(#var1 = :val1)'");
    //                withExpectedResult("le", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=freight} valueMap={:val1=ORDER, :val2=10} filterExpression='(#var2 <= :val2)' keyConditionExpression='(#var1 = :val1)'");
    //                withExpectedResult("gt", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=freight} valueMap={:val1=ORDER, :val2=3.67} filterExpression='(#var2 > :val2)' keyConditionExpression='(#var1 = :val1)'");
    //                withExpectedResult("ge", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=freight} valueMap={:val1=ORDER, :val2=3.67} filterExpression='(#var2 >= :val2)' keyConditionExpression='(#var1 = :val1)'");
    //                withExpectedResult("in", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=ls1} valueMap={:val1=ORDER, :val2=Reims, :val3=Charleroi} filterExpression='(#var2 IN (:val2, :val3))' keyConditionExpression='(#var1 = :val1)'");
    //                withExpectedResult("out", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=ls1} valueMap={:val1=ORDER, :val2=Reims, :val3=Charleroi} filterExpression='(NOT #var2 IN (:val2, :val3))' keyConditionExpression='(#var1 = :val1)'");
    //                withExpectedResult("and", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=ls1, #var3=shipCountry} valueMap={:val1=ORDER, :val2=Lyon, :val3=France} filterExpression='(#var2 = :val2) and (#var3 = :val3)' keyConditionExpression='(#var1 = :val1)'");
    //                withExpectedResult("or", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=ls1, #var3=ls1} valueMap={:val1=ORDER, :val2=Reims, :val3=Charleroi} filterExpression='((#var2 = :val2) or (#var3 = :val3))' keyConditionExpression='(#var1 = :val1)'");
    //                withExpectedResult("not", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=ls1, #var3=ls1} valueMap={:val1=ORDER, :val2=Reims, :val3=Charleroi} filterExpression='(NOT ((#var2 = :val2) or (#var3 = :val3)))' keyConditionExpression='(#var1 = :val1)'");
    //                withExpectedResult("as", "UNSUPPORTED");
    //                withExpectedResult("includes", "QuerySpec:'gs3' nameMap={#var1=sk} valueMap={:val1=ORDER} projectionExpression='shipCountry,ls1' keyConditionExpression='(#var1 = :val1)'");
    //                withExpectedResult("distinct", "UNSUPPORTED");
    //                withExpectedResult("count1", "UNSUPPORTED");
    //                withExpectedResult("count2", "UNSUPPORTED");
    //                withExpectedResult("count3", "UNSUPPORTED");
    //                withExpectedResult("countAs", "UNSUPPORTED");
    //                withExpectedResult("sum", "UNSUPPORTED");
    //                withExpectedResult("sumAs", "UNSUPPORTED");
    //                withExpectedResult("sumIf", "UNSUPPORTED");
    //                withExpectedResult("min", "UNSUPPORTED");
    //                withExpectedResult("max", "UNSUPPORTED");
    //                withExpectedResult("groupCount", "UNSUPPORTED");
    //                withExpectedResult("offset", "UNSUPPORTED");
    //                withExpectedResult("limit", "DynamoDb: QuerySpec:'gs3' maxPageSize=7 nameMap={#var1=sk} valueMap={:val1=ORDER} keyConditionExpression='(#var1 = :val1)'");
    //                withExpectedResult("page", "UNSUPPORTED");
    //                withExpectedResult("pageNum", "UNSUPPORTED");
    //
    //                withExpectedResult("after", "QuerySpec:'gs3' nameMap={#var1=sk} valueMap={:val1=ORDER} exclusiveStartKey='[{sk: ORDER}, {hk: 10254}] keyConditionExpression='(#var1 = :val1)'");
    //
    //                withExpectedResult("sort", "QuerySpec:'gs3' nameMap={#var1=sk} valueMap={:val1=ORDER} keyConditionExpression='(#var1 = :val1)' scanIndexForward=false");
    //                withExpectedResult("order", "DynamoDb: QuerySpec:'gs2' nameMap={#var1=gs2hk} valueMap={:val1=12345} keyConditionExpression='(#var1 = :val1)' scanIndexForward=false");
    //
    //
    //                withExpectedResult("onToManyExistsEq", "UNSUPPORTED");
    //                withExpectedResult("onToManyNotExistsNe", "UNSUPPORTED");
    //                withExpectedResult("manyToOneExistsEq", "UNSUPPORTED");
    //                withExpectedResult("manyToOneNotExistsNe", "UNSUPPORTED");
    //                withExpectedResult("manyTManyNotExistsNe", "UNSUPPORTED");
    //
    //                .withTest("A_scanWhenUnindexedFieldProvided", "orders?shipPostalCode=30305");
    //                withExpectedResult("A_scanWhenUnindexedFieldProvided", "ScanSpec nameMap={#var1=shipPostalCode} valueMap={:val1=30305} filterExpression='(#var1 = :val1)'");
    //
    //                .withTest("B_scanWhenOnlyLocalSecondaryProvided", "orders?eq(shipName,something)");
    //                withExpectedResult("B_scanWhenOnlyLocalSecondaryProvided", "ScanSpec nameMap={#var1=ls2} valueMap={:val1=something} filterExpression='(#var1 = :val1)'");
    //
    //                .withTest("C_queryPIWhenHashKeyProvided", "orders?eq(orderId, 12345)");
    //                withExpectedResult("C_queryPIWhenHashKeyProvided", "QuerySpec:'Primary Index' nameMap={#var1=hk} valueMap={:val1=12345} keyConditionExpression='(#var1 = :val1)'");
    //
    //                .withTest("D_getWhenHashAndSortProvided", "orders?eq(orderId, 12345)&eq(type, 'ORDER')");
    //                withExpectedResult("D_getWhenHashAndSortProvided", "GetItemSpec:'Primary Index' key: [{hk: 12345}, {sk: ORDER}]");
    //
    //                .withTest("E_queryPi", "orders?eq(orderId, 12345)&gt(type, 'AAAAA')");
    //                withExpectedResult("E_queryPi", "QuerySpec:'Primary Index' nameMap={#var1=hk, #var2=sk} valueMap={:val1=12345, :val2=AAAAA} keyConditionExpression='(#var1 = :val1) and (#var2 > :val2)'");
    //
    //                .withTest("F_queryPi", "orders?eq(orderId, 12345)&gt(type, 'AAAAA')&gt(ShipCity,Atlanta)");
    //                withExpectedResult("F_queryPi", "QuerySpec:'Primary Index' nameMap={#var1=hk, #var2=sk, #var3=ls1} valueMap={:val1=12345, :val2=AAAAA, :val3=Atlanta} filterExpression='(#var3 > :val3)' keyConditionExpression='(#var1 = :val1) and (#var2 > :val2)'");
    //
    //                .withTest("G_queryPi", "orders?eq(orderId, 12345)&sw(type, 'ORD')");
    //                withExpectedResult("G_queryPi", "QuerySpec:'Primary Index' nameMap={#var1=hk, #var2=sk} valueMap={:val1=12345, :val2=ORD} keyConditionExpression='(#var1 = :val1) and begins_with(#var2,:val2)'");
    //
    //                .withTest("H_queryLs1WhenHkEqAndLs1Eq", "orders?eq(orderId, 12345)&sw(type, 'ORD')&eq(ShipCity,Atlanta)");
    //                withExpectedResult("H_queryLs1WhenHkEqAndLs1Eq", "QuerySpec:'ls1' nameMap={#var1=hk, #var2=ls1, #var3=sk} valueMap={:val1=12345, :val2=Atlanta, :val3=ORD} filterExpression='begins_with(#var3,:val3)' keyConditionExpression='(#var1 = :val1) and (#var2 = :val2)'");
    //
    //                .withTest("I_queryGs1When", "orders?eq(orderId, 12345)&sw(type,ORD)&eq(employeeId,9999)&eq(orderDate,'2013-01-08')");
    //                withExpectedResult("I_queryGs1When", "QuerySpec:'gs1' nameMap={#var4=sk, #var1=gs1hk, #var2=gs1sk, #var3=hk} valueMap={:val1=9999, :val2=2013-01-08, :val3=12345, :val4=ORD} filterExpression='(#var3 = :val3) and begins_with(#var4,:val4)' keyConditionExpression='(#var1 = :val1) and (#var2 = :val2)'");
    //
    //                .withTest("K_queryGs3", "orders?gt(orderId, 12345)&eq(type,ORDER)");
    //                withExpectedResult("K_queryGs3", "QuerySpec:'gs3' nameMap={#var1=sk, #var2=hk} valueMap={:val1=ORDER, :val2=12345} keyConditionExpression='(#var1 = :val1) and (#var2 > :val2)'");
    //
    //                .withTest("M_queryGs2WhenGs2HkEq", "orders?eq(customerId,1234)");
    //                withExpectedResult("M_queryGs2WhenGs2HkEq", "QuerySpec:'gs2' nameMap={#var1=gs2hk} valueMap={:val1=1234} keyConditionExpression='(#var1 = :val1)'");
    //        //.withTest("", "")withExpectedResult("",  "");
    //
    //        ;
    //    }
}
