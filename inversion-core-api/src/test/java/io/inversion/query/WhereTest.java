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
package io.inversion.query;

import io.inversion.query.Query;
import io.inversion.query.Where;
import io.inversion.rql.Rql;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WhereTest {

    @Test
    public void isInvalidColumn() {
        Where where = new Where(new Query());
        assertFalse(where.isInvalidColumn(Rql.parse("function(column,1,2,3)")));
        assertFalse(where.isInvalidColumn(Rql.parse("function(column_name,1,2,3)")));
        assertTrue(where.isInvalidColumn(Rql.parse("function(_column,1,2,3)")));
        assertTrue(where.isInvalidColumn(Rql.parse("function(col-umn,1,2,3)")));
    }

}
