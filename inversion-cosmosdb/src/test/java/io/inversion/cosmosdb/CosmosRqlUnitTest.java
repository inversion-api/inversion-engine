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
package io.inversion.cosmosdb;

import io.inversion.Db;
import io.inversion.rql.AbstractRqlTest;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class CosmosRqlUnitTest extends AbstractRqlTest {

    public CosmosRqlUnitTest() {
        super("northwind/cosmosdb/", "cosmosdb");

//        - likeMiddle - orders?like(shipCountry,F*ance) - 400 Bad Request - 400 Bad Request - The 'like' RQL operator for CosmosDb expects a single wildcard at the beginning OR the end of a value.  CosmosDb does not really support 'like' but compatible 'like' statements are turned into 'sw' or 'ew' statements that are supported.
//        - w - orders?w(shipCountry,ance) - 400 Bad Request - 400 Bad Request - CosmosDb supports 'sw' and 'ew' but not 'w' or 'wo' functions.
//                - wo - orders?wo(shipCountry,ance) - 400 Bad Request - 400 Bad Request - CosmosDb supports 'sw' and 'ew' but not 'w' or 'wo' functions.
        
        withExpectedResult("eq", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"orderId\"] = @orderId1 AND Northwind[\"shipCountry\"] = @shipCountry2 AND Northwind[\"type\"] = @type3 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@orderId1=10248}, {@shipCountry2=France}, {@type3=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("ne", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 AND (NOT (Northwind[\"shipCountry\"] = @shipCountry2)) ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}, {@shipCountry2=France}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("n", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 AND IS_NULL (Northwind[\"shipRegion\"]) ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("nn", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 AND Northwind[\"shipRegion\"] <> null ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("emp", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE (Northwind[\"shipRegion\"] IS NULL OR Northwind[\"shipRegion\"] = '') AND Northwind[\"type\"] = @type1 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("nemp", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 AND (Northwind[\"shipRegion\"] IS NOT NULL AND Northwind[\"shipRegion\"] != '') ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("likeMiddle", "400 Bad Request - The 'like' RQL operator for CosmosDb expects a single wildcard at the beginning OR the end of a value.  CosmosDb does not really support 'like' but compatible 'like' statements are turned into 'sw' or 'ew' statements that are supported.");
//                                                             "400 Bad Request - 400 Bad Request - The 'like' RQL operator for CosmosDb expects a single wildcard at the beginning OR the end of a value.  CosmosDb does not really support 'like' but compatible 'like' statements are turned into 'sw' or 'ew' statements that are supported."
        
        withExpectedResult("likeStartsWith", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 AND STARTSWITH (Northwind[\"shipCountry\"], @shipCountry2) ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}, {@shipCountry2=Franc}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("likeEndsWith", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 AND ENDSWITH (Northwind[\"shipCountry\"], @shipCountry2) ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}, {@shipCountry2=ance}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("sw", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 AND STARTSWITH (Northwind[\"shipCountry\"], @shipCountry2) ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}, {@shipCountry2=Franc}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("ew", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 AND ENDSWITH (Northwind[\"shipCountry\"], @shipCountry2) ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}, {@shipCountry2=nce}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("w", "400 Bad Request - CosmosDb supports 'sw' and 'ew' but not 'w' or 'wo' functions.");
        withExpectedResult("wo", "400 Bad Request - CosmosDb supports 'sw' and 'ew' but not 'w' or 'wo' functions.");
        withExpectedResult("lt", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 AND Northwind[\"freight\"] < @freight2 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}, {@freight2=10}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("le", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 AND Northwind[\"freight\"] <= @freight2 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}, {@freight2=10}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("gt", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 AND Northwind[\"freight\"] > @freight2 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}, {@freight2=3.67}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("ge", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 AND Northwind[\"freight\"] >= @freight2 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}, {@freight2=3.67}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("in", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 AND Northwind[\"shipCity\"] IN(@shipCity2, @shipCity3) ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}, {@shipCity2=Reims}, {@shipCity3=Charleroi}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("out", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 AND Northwind[\"shipCity\"] NOT IN(@shipCity2, @shipCity3) ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}, {@shipCity2=Reims}, {@shipCity3=Charleroi}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("and", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"shipCity\"] = @shipCity1 AND Northwind[\"shipCountry\"] = @shipCountry2 AND Northwind[\"type\"] = @type3 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@shipCity1=Lyon}, {@shipCountry2=France}, {@type3=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("or", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 AND (Northwind[\"shipCity\"] = @shipCity2 OR Northwind[\"shipCity\"] = @shipCity3) ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}, {@shipCity2=Reims}, {@shipCity3=Charleroi}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("not", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 AND NOT ((Northwind[\"shipCity\"] = @shipCity2 OR Northwind[\"shipCity\"] = @shipCity3)) ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}, {@shipCity2=Reims}, {@shipCity3=Charleroi}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("as", "CosmosDb: SqlQuerySpec=SELECT *, Northwind[\"orderId\"] AS \"order_identifier\" FROM Northwind WHERE Northwind[\"type\"] = @type1 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("includes", "CosmosDb: SqlQuerySpec=SELECT Northwind[\"shipCountry\"], Northwind[\"shipCity\"], Northwind[\"type\"], Northwind[\"orderId\"] FROM Northwind WHERE Northwind[\"type\"] = @type1 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");


        withExpectedResult("distinct", "CosmosDb: SqlQuerySpec=SELECT DISTINCT Northwind[\"shipCountry\"], Northwind[\"type\"], Northwind[\"orderId\"] FROM Northwind WHERE Northwind[\"type\"] = @type1 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");

        withExpectedResult("count1", "CosmosDb: SqlQuerySpec=SELECT COUNT(*) FROM Northwind WHERE Northwind[\"type\"] = @type1 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("count2", "CosmosDb: SqlQuerySpec=SELECT COUNT(@null1) FROM Northwind WHERE Northwind[\"type\"] = @type2 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@null1=1}, {@type2=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("count3", "CosmosDb: SqlQuerySpec=SELECT COUNT(Northwind[\"shipRegion\"]) FROM Northwind WHERE Northwind[\"type\"] = @type1 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("countAs", "CosmosDb: SqlQuerySpec=SELECT COUNT(*) AS \"countOrders\" FROM Northwind WHERE Northwind[\"type\"] = @type1 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("sum", "CosmosDb: SqlQuerySpec=SELECT SUM(Northwind[\"freight\"]) FROM Northwind WHERE Northwind[\"type\"] = @type1 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("sumAs", "CosmosDb: SqlQuerySpec=SELECT SUM(Northwind[\"freight\"]) AS \"Sum Freight\" FROM Northwind WHERE Northwind[\"type\"] = @type1 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("sumIf", "CosmosDb: SqlQuerySpec=SELECT SUM(CASE WHEN Northwind[\"shipCountry\"] = @shipCountry1 THEN 1 ELSE 0 END) AS \"French Orders\" FROM Northwind WHERE Northwind[\"type\"] = @type2 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@shipCountry1=France}, {@type2=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("min", "CosmosDb: SqlQuerySpec=SELECT MIN(Northwind[\"freight\"]) FROM Northwind WHERE Northwind[\"type\"] = @type1 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("max", "CosmosDb: SqlQuerySpec=SELECT MAX(Northwind[\"freight\"]) FROM Northwind WHERE Northwind[\"type\"] = @type1 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("groupCount", "CosmosDb: SqlQuerySpec=SELECT Northwind[\"shipCountry\"], COUNT(*) AS \"countryCount\", Northwind[\"type\"], Northwind[\"orderId\"] FROM Northwind WHERE Northwind[\"type\"] = @type1 GROUP BY Northwind[\"shipCountry\"] ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("offset", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 ORDER BY Northwind[\"id\"] ASC OFFSET 3 LIMIT 100 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("limit", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 7 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("page", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 ORDER BY Northwind[\"id\"] ASC OFFSET 14 LIMIT 7 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("pageNum", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 ORDER BY Northwind[\"id\"] ASC OFFSET 14 LIMIT 7 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("after", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 ORDER BY Northwind[\"id\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("sort", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 ORDER BY Northwind[\"shipCountry\"] DESC, Northwind[\"shipCity\"] ASC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("order", "CosmosDb: SqlQuerySpec=SELECT * FROM Northwind WHERE Northwind[\"type\"] = @type1 ORDER BY Northwind[\"shipCountry\"] ASC, Northwind[\"shipCity\"] DESC OFFSET 0 LIMIT 100 Parameters=[{@type1=orders}] CosmosQueryRequestOptions={enableCrossPartitionQuery=false}");

        withExpectedResult("onToManyExistsEq", "UNSUPPORTED");
        withExpectedResult("onToManyNotExistsNe", "UNSUPPORTED");
        withExpectedResult("manyToOneExistsEq", "UNSUPPORTED");
        withExpectedResult("manyToOneNotExistsNe", "UNSUPPORTED");
        withExpectedResult("manyTManyNotExistsNe", "UNSUPPORTED");
        withExpectedResult("eqNonexistantColumn", "UNSUPPORTED");
    }

    @Override
    public Db buildDb() {
        return CosmosDbFactory.buildDb();
    }

}
