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
package io.inversion.jdbc.sqlserver;

import io.inversion.rql.RqlValidationSuite;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class SqlServerRqlIntegTest extends SqlServerRqlUnitTest {

    public SqlServerRqlIntegTest() throws Exception {
        super();
        withExpectedResult("manyTManyNotExistsNe",
                "SELECT \"Employees\".* FROM \"Employees\" WHERE NOT EXISTS (SELECT 1 FROM \"Order Details\" \"~~relTbl_Order Details\", \"employeeorderdetails\" \"~~lnkTbl_employeeorderdetails\" WHERE \"Employees\".\"EmployeeID\" = \"~~lnkTbl_employeeorderdetails\".\"EmployeeID\" AND \"~~lnkTbl_employeeorderdetails\".\"OrderID\" = \"~~relTbl_Order Details\".\"OrderID\" AND \"~~lnkTbl_employeeorderdetails\".\"ProductID\" = \"~~relTbl_Order Details\".\"ProductID\" AND \"~~relTbl_Order Details\".\"Quantity\" = ?) ORDER BY \"Employees\".\"EmployeeID\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[12]");
    }
}