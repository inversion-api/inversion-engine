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
package io.inversion.rql;

import io.inversion.*;
import io.inversion.utils.Utils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public abstract class AbstractRqlTest implements AbstractEngineTest {

    protected String              urlPrefix         = null;
    protected Engine              engine            = null;
    protected String              type              = null;

    protected Map<String, String> testRequests      = new LinkedHashMap<>();
    protected Map<String, String> expectedResponses = new HashMap<>();

    public AbstractRqlTest(String urlPrefix, String type) {
        setUrlPrefix(urlPrefix);
        setType(type);

        withTestRequest("eq", "orders?eq(orderID, 10248)&eq(shipCountry,France)");
        withTestRequest("ne", "orders?ne(shipCountry,France)");

        withTestRequest("n", "orders?n(shipRegion)");
        withTestRequest("nn", "orders?nn(shipRegion)");
        withTestRequest("emp", "orders?emp(shipRegion)");
        withTestRequest("nemp", "orders?nemp(shipRegion)");

        withTestRequest("likeMiddle", "orders?like(shipCountry,F*ance)");
        withTestRequest("likeStartsWith", "orders?like(shipCountry,Franc*)");
        withTestRequest("likeEndsWith", "orders?like(shipCountry,*ance)");
        withTestRequest("sw", "orders?sw(shipCountry,Franc)");
        withTestRequest("ew", "orders?ew(shipCountry,nce)");
        withTestRequest("w", "orders?w(shipCountry,ance)");
        withTestRequest("wo", "orders?wo(shipCountry,ance)");

        withTestRequest("lt", "orders?lt(freight,10)");
        withTestRequest("le", "orders?le(freight,10)");
        withTestRequest("gt", "orders?gt(freight,3.67)");
        withTestRequest("ge", "orders?ge(freight,3.67)");
        withTestRequest("in", "orders?in(shipCity,Reims,Charleroi)");
        withTestRequest("out", "orders?out(shipCity,Reims,Charleroi)");

        withTestRequest("and", "orders?and(eq(shipCity,Lyon),eq(shipCountry,France))");
        withTestRequest("or", "orders?or(eq(shipCity, Reims),eq(shipCity,Charleroi))");
        withTestRequest("not", "orders?not(or(eq(shipCity, Reims),eq(shipCity,Charleroi)))");

        withTestRequest("as", "orders?as(orderid,order_identifier)");
        withTestRequest("includes", "orders?includes(shipCountry,shipCity)");
        withTestRequest("distinct", "orders?distinct&includes=shipCountry");

        withTestRequest("count1", "orders?count(*)");
        withTestRequest("count2", "orders?count(1)");
        withTestRequest("count3", "orders?count(shipRegion)");//in the data some shipRegions are null so this would be fewer than count(*) or count(1)
        withTestRequest("countAs", "orders?as(count(*),countOrders)");
        //-- this is commented out because Select.java translates this to as(count(shipCountry), numRows) which is tested above
        //-- withTest("countAs2", "orders?count(shipCountry,numRows)"); //

        withTestRequest("sum", "orders?sum(freight)");
        withTestRequest("sumAs", "orders?as(sum(freight),'Sum Freight')");

        //-- this is commented out because Select.java translates this to as(sum(freight), sumFreight) which is tested above
        //-- withTest("sumAs", "orders?sum(freight,sumFreight)");

        withTestRequest("sumIf", "orders?as(sum(if(eq(shipCountry,France),1,0)), 'French Orders')");
        withTestRequest("min", "orders?min(freight)");
        withTestRequest("max", "orders?max(freight)");
        //withTest("if", "orders?");
        withTestRequest("groupCount", "orders?group(shipCountry)&as(count(*),countryCount)&includes=shipCountry,countryCount");

        //      withTest("aggregate", "orders?");
        //      withTest("function", "orders?");
        //      withTest("countascol", "orders?");
        //      withTest("rowcount", "orders?");

        withTestRequest("offset", "orders?offset=3");
        withTestRequest("limit", "orders?limit=7");
        withTestRequest("page", "orders?pageSize=7&page=3");
        withTestRequest("pageNum", "orders?pageSize=7&pageNum=3");
        withTestRequest("after", "orders?after=10248");

        withTestRequest("sort", "orders?sort(-shipCountry,shipCity)");
        withTestRequest("order", "orders?order(shipCountry,-shipCity)");

        withTestRequest("onToManyExistsEq", "employees?eq(reportsTo.firstName,Andrew)");
        withTestRequest("onToManyNotExistsNe", "employees?ne(reportsTo.firstName,Andrew)");

        withTestRequest("manyToOneExistsEq", "employees?eq(employees.firstName,Nancy)");
        withTestRequest("manyToOneNotExistsNe", "employees?ne(employees.firstName,Nancy)");

        withTestRequest("manyTManyNotExistsNe", "employees?ne(orderdetails.quantity,12)");
        withTestRequest("eqNonexistantColumn", "orders?ge(orderId, 1000)&eq(nonexistantColumn,12)");

        //        withExpectedResult("eq", "PUT YOUR SQL HERE");//
        //        withExpectedResult("ne", "");//
        //        withExpectedResult("n", "");
        //        withExpectedResult("nn", "");
        //        withExpectedResult("emp", "");
        //        withExpectedResult("nemp", "");
        //        withExpectedResult("likeMiddle", "");
        //        withExpectedResult("likeStartsWith", "");
        //        withExpectedResult("likeEndsWith", "");
        //        withExpectedResult("sw", "");
        //        withExpectedResult("ew", "");
        //        withExpectedResult("w", "");
        //        withExpectedResult("wo", "");
        //        withExpectedResult("lt", "");
        //        withExpectedResult("le", "");
        //        withExpectedResult("gt", "");
        //        withExpectedResult("ge", "");
        //        withExpectedResult("in", "");
        //        withExpectedResult("out", "");
        //        withExpectedResult("and", "");
        //        withExpectedResult("or", "");
        //        withExpectedResult("not", "");
        //        withExpectedResult("as", "");
        //        withExpectedResult("includes", "");
        //        withExpectedResult("distinct", "");
        //        withExpectedResult("count1", "");
        //        withExpectedResult("count2", "");
        //        withExpectedResult("count3", "");
        //        withExpectedResult("countAs", "");
        //        withExpectedResult("sum", "");
        //        withExpectedResult("sumAs", "");
        //        withExpectedResult("sumIf", "");
        //        withExpectedResult("min", "");
        //        withExpectedResult("max", "");
        //        withExpectedResult("groupCount", "");
        //        withExpectedResult("offset", "");
        //        withExpectedResult("limit", "");
        //        withExpectedResult("page", "");
        //        withExpectedResult("pageNum", "");
        //        withExpectedResult("after", "");
        //        withExpectedResult("sort", "");
        //        withExpectedResult("order", "");
        //        withExpectedResult("onToManyExistsEq", "");
        //        withExpectedResult("onToManyNotExistsNe", "");
        //        withExpectedResult("manyToOneExistsEq", "");
        //        withExpectedResult("manyToOneNotExistsNe", "");
        //        withExpectedResult("manyTManyNotExistsNe", "");
        //        withExpectedResult("eqNonexistantColumn", "");
    }

    @Test
    public void runTests() throws Exception {

        LinkedHashMap<String, String> failures = new LinkedHashMap<>();

        for (String testKey : testRequests.keySet()) {
            String queryString = testRequests.get(testKey);

            if (Utils.empty(testKey) || Utils.empty(queryString))
                continue;

            String expected = expectedResponses.get(testKey);
            if ("UNSUPPORTED".equalsIgnoreCase(expected))
                continue;

            System.out.println("\r\nTESTING: " + testKey + " - " + queryString);

            Results.LAST_QUERY = null;

            Response res = runTest(getEngine(), urlPrefix, testKey, queryString);

            String maybeMatch = !Utils.empty(Results.LAST_QUERY) ? Results.LAST_QUERY : res.findString("message");

            if (Utils.empty(expected))
                expected = "YOU NEED TO SUPPLY A MATCH FOR THIS TEST: " + maybeMatch;

            if (!verifyTest(testKey, queryString, expected, res)) {
                System.out.println("FAILED: " + testKey);
                System.out.println(" - expected: " + expected);
                System.out.println(" - received: " + Results.LAST_QUERY);
                res.dump();
                failures.put(testKey, res.getStatus() + " - " + maybeMatch);
            }
        }

        if (failures.size() > 0) {
            System.out.println("Failed cases...");
            for (String key : failures.keySet()) {
                String failure = null;
                failure = failures.get(key);

                int idx = failure.indexOf("\"message\"");
                if (idx > -1) {
                    int idx2 = failure.indexOf("\n", idx);
                    failure = failure.substring(idx + 12, idx2);
                }

                System.out.println("  - " + key + " - " + testRequests.get(key) + " - " + failure);
            }

            throw new RuntimeException("Failed...");
        }
    }

    /**
     * Override me to add custom test execution handling.
     * 
     * @param engine
     * @param urlPrefix
     * @param testKey
     * @param queryString
     * @return
     */
    protected Response runTest(Engine engine, String urlPrefix, String testKey, String queryString) {
        return engine.get(urlPrefix + queryString);
    }

    /**
     * Override me to add additional special case validations per test.
     * <p>
     * By default tests pass if the response debug dump contains the test result string...the string match is case insensitive
     *
     * @param testKey
     * @param queryString
     * @param res
     * @return
     */
    protected boolean verifyTest(String testKey, String queryString, String expectedMatch, Response res) {

        if (isIntegTest()) {
            if (!res.hasStatus(200, 404) && !expectedMatch.startsWith(res.getStatusCode() + " "))
                return false;
        }

        String debug = res.getDebug();
        if (debug == null)
            return false;

        debug = debug.replace("SQL_CALC_FOUND_ROWS ", "").toLowerCase();

        return debug.indexOf(expectedMatch.toLowerCase()) > -1;
    }

    public AbstractRqlTest withTestRequest(String testKey, String testRql) {
        testRequests.put(testKey, testRql);
        return this;
    }

    public AbstractRqlTest withExpectedResult(String testKey, String queryOutput) {
        expectedResponses.put(testKey, queryOutput);
        return this;
    }

    public Map<String, String> getTestRequests() {
        return testRequests;
    }

    public Map<String, String> getExpectedResponses() {
        return expectedResponses;
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public void setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
