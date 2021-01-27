/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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
package io.inversion.action.misc;

import io.inversion.*;
import io.inversion.utils.Path;
import io.inversion.utils.Utils;

import java.io.InputStream;

public class FileAction extends Action<io.inversion.action.misc.CsvAction> {

    protected String baseDir = null;

    public void doGet(Request req, Response res) throws ApiException {

        String filePath = req.getEndpointPath().toString();
        if(filePath.startsWith("/"))
            filePath = filePath.substring(1, filePath.length());

        String fullPath = filePath;
        if(baseDir != null)
            fullPath = new Path(baseDir, filePath).toString();

        InputStream is = Utils.findInputStream(fullPath);
        if(is == null)
            throw ApiException.new404NotFound("File '{}' could not be found", filePath);

        String txt = Utils.read(is);
        res.withText(txt);
    }

    public String getBaseDir() {
        return baseDir;
    }

    public FileAction withBaseDir(String baseDir) {
        this.baseDir = baseDir;
        return this;
    }
}