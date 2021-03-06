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
package io.inversion.jdbc;

import io.inversion.action.db.AbstractDbDeleteActionIntegTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractJdbcDbDeleteActionIntegTest extends AbstractDbDeleteActionIntegTest implements AbstractJdbcDbEngineTest {

    public AbstractJdbcDbDeleteActionIntegTest(String dbType) {
        super(dbType);
    }

    @BeforeEach
    public void beforeEach() {
        beforeAll_initializeEngine();
    }

    @AfterEach
    public void afterEach() {
        afterAll_finalizeEngine();
    }

}
