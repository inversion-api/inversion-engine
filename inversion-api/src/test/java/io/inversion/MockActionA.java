/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion;

import io.inversion.utils.JSNode;

public class MockActionA extends Action<MockActionA> {

    public MockActionA() {

    }


    public MockActionA(String methods, String... includePaths) {
        withIncludeOn(methods, includePaths);
    }

    @Override
    public void run(Request req, Response res) throws ApiException {
        if (req.isMethod("get")) {
            res.withRecord(new JSNode("primaryKey", 1, "firstName", "tester1", "className", getClass().getSimpleName()));
        } else if (req.isMethod("put", "post")) {

        } else if (req.isMethod("delete")) {

        }
    }

    public String toString() {
        return getClass().getSimpleName();
    }

}
