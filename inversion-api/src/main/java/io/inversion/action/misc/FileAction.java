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
import io.inversion.utils.StreamBuffer;
import io.inversion.utils.Utils;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class FileAction <A extends FileAction> extends Action<A> {
    protected String baseDir = null;
    protected Set<String> files = new HashSet<>();

    public void doGet(Request req, Response res) throws ApiException {
        serveFile(req, res);
    }

    protected void serveFile(Request req, Response res){
        boolean filterMode = req.getOp() == null;
        Path path = req.getOperationPath();
        String filePath = null;
        if(path == null){
            path = req.getUrl().getPath();
            filePath = path.last();
        }
        else{
            filePath = path.toString();
        }
        serveFile(req, res, filePath, filterMode);
    }

    protected void serveFile(Request req, Response res, String filePath, boolean filterMode){
        if(filePath != null) {
            InputStream is = findStream(filePath);
            if (is != null) {
                res.withStream(new StreamBuffer(is));
                if (filterMode)
                    req.getChain().cancel();

                return;
            }
        }
        if(!filterMode)
            throw ApiException.new404NotFound("File '{}' could not be found", filePath);
    }

    protected InputStream findStream(String filePath){
        if(baseDir != null)
            filePath = new Path(baseDir, filePath).toString();

        return Utils.findInputStream(this, filePath);
    }

    public boolean canServe(String filePath){
        if(filePath == null)
            return false;

        filePath = new Path(filePath).toString();
        String fullPath = filePath;
        if(baseDir != null)
            fullPath = new Path(baseDir, fullPath).toString();

        if(files.size() > 0){
            if(!(files.contains(fullPath.toLowerCase()) || files.contains(filePath.toLowerCase())))
                return false;
        }
        return true;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public FileAction withBaseDir(String baseDir) {
        this.baseDir = baseDir;
        return this;
    }

    public Set<String> getFiles() {
        return files;
    }

    public FileAction withFiles(String... files) {
        for(String file : files)
            this.files.add(file.toLowerCase());
        return this;
    }

}