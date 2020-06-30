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
package io.inversion.action.db;

import io.inversion.AbstractEngineTest;
import io.inversion.Db;
import io.inversion.Engine;
import io.inversion.Response;

public abstract class AbstractDbActionIntegTest implements AbstractEngineTest {
    protected Engine engine = null;
    protected Db     db     = null;
    protected String type   = null;

    public AbstractDbActionIntegTest(String type) {
        this.type = type;
    }

    protected Engine engine() throws Exception {
        return engine;
    }

    protected String collectionPath() {
        return "northwind/" + type + "/";
    }

    @Override
    public Db getDb() {
        return db;
    }

    @Override
    public void setDb(Db db) {
        this.db = db;

    }

    @Override
    public Engine getEngine() {
        return engine;
    }

    @Override
    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the last response handled by the engine.
     * This is needed for subclasses to decorate test methods
     *
     * @return
     */
    protected Response response() throws Exception {
        return engine().getLastResponse();
    }

    protected String url(String path) {
        if (path.startsWith("http"))
            return path;

        String cp = collectionPath();

        if (cp.length() == 0)
            return path;

        if (!cp.endsWith("/"))
            cp += "/";

        while (path.startsWith("/"))
            path = path.substring(1, path.length());

        //      if (path.indexOf(cp) > -1 || path.startsWith("http"))
        //         return path;

        return "http://localhost/" + cp + path;
    }
}
