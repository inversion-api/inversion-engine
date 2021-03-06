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
package io.inversion.jdbc.postgres;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class PostgresRqlIntegTest extends PostgresRqlUnitTest {

    public PostgresRqlIntegTest() throws Exception {
        super();
        withExpectedResult("manyTManyNotExistsNe",
                "SELECT \"employees\".* FROM \"employees\" WHERE NOT EXISTS (SELECT 1 FROM \"order_details\" \"~~relTbl_order_details\", \"employeeorderdetails\" \"~~lnkTbl_employeeorderdetails\" WHERE \"employees\".\"EmployeeID\" = \"~~lnkTbl_employeeorderdetails\".\"EmployeeID\" AND \"~~lnkTbl_employeeorderdetails\".\"OrderID\" = \"~~relTbl_order_details\".\"OrderID\" AND \"~~lnkTbl_employeeorderdetails\".\"ProductID\" = \"~~relTbl_order_details\".\"ProductID\" AND \"~~relTbl_order_details\".\"Quantity\" = ?) ORDER BY \"employees\".\"EmployeeID\" ASC LIMIT 100 OFFSET 0 args=[12]");
    }
}
