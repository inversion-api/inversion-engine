/*
 * Copyright (c) 2015-2022 Rocket Partners, LLC
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

import java.io.InputStream;

public class Upload {

    String      partName;
    String      fileName;
    long        fileSize;
    String      mimeType;
    InputStream inputStream;

    public Upload(String partName, String fileName, long fileSize, String mimeType, InputStream inputStream) {
        this.partName = partName;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.inputStream = inputStream;
    }

    public String getPartName() {
        return partName;
    }

    public Upload withPartName(String partName) {
        this.partName = partName;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public Upload withFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public long getFileSize() {
        return fileSize;
    }

    public Upload withFileSize(long fileSize) {
        this.fileSize = fileSize;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Upload withMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public Upload withInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }
}