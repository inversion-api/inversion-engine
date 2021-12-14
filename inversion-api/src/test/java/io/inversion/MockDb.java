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
package io.inversion;

import io.inversion.rql.Term;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MockDb extends Db<MockDb> {

    /**
     * Generic property for configuration test cases to set
     */
    String property1 = null;

    /**
     * Generic property for configuration test cases to set
     */
    String property2 = null;

    /**
     * Generic property for configuration test cases to set
     */
    String property3 = null;

    /**
     * Generic property for testing configuration secret masking
     */
    String password = null;

    public MockDb(){

    }

    public MockDb(String name){
        withName(name);
    }

    @Override
    public Results doSelect(Collection table, List<Term> columnMappedTerms) throws ApiException {
        return new Results(null);
    }

    @Override
    public List<String> doUpsert(Collection table, List<Map<String, Object>> rows) throws ApiException {
        return Collections.EMPTY_LIST;
    }

    @Override
    public void doDelete(Collection table, List<Map<String, Object>> indexValues) throws ApiException {

    }

    public String getProperty1() {
        return property1;
    }

    public MockDb withProperty1(String property1) {
        this.property1 = property1;
        return this;
    }

    public String getProperty2() {
        return property2;
    }

    public MockDb withProperty2(String property2) {
        this.property2 = property2;
        return this;
    }

    public String getProperty3() {
        return property3;
    }

    public MockDb withProperty3(String property3) {
        this.property3 = property3;
        return this;
    }
}
