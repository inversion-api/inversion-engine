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
package io.inversion;

import io.inversion.Request.Validation;
import io.inversion.utils.JSArray;
import io.inversion.utils.JSNode;
import io.inversion.utils.Utils;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Response {

    protected long startAt = System.currentTimeMillis();
    protected long endAt = -1;

    protected Request request = null;
    protected final ArrayListValuedHashMap<String, String> headers = new ArrayListValuedHashMap<>();
    protected final List<Change>  changes = new ArrayList<>();
    protected final StringBuilder debug   = new StringBuilder();
    protected String url = null;
    protected Chain chain = null;
    protected int    statusCode  = 200;
    protected String statusMesg  = "OK";
    protected String redirect    = null;
    protected String        contentType = null;
    protected StringBuilder out         = new StringBuilder();
    protected JSNode        json        = new JSNode("meta", new JSNode("createdOn", Utils.formatIso8601(new Date())), "data", new JSArray());
    protected String        text        = null;
    protected String fileName = null;
    protected File   file     = null;
    protected Throwable error = null;
    protected String contentRangeUnit  = null;
    protected long   contentRangeStart = -1;
    protected long   contentRangeEnd   = -1;
    protected long   contentRangeSize  = -1;

    public Response() {

    }

    public Response(String url) {
        withUrl(url);
    }

    public long getStartAt() { return startAt;}

    public Response withStartAt(long startAt){
        this.startAt = startAt;
        return this;
    }

    public long getEndAt(){return endAt;}

    public Response withEndAt(long endAt){
        this.endAt = endAt;
        return this;
    }

    public boolean hasStatus(int... statusCodes) {
        for (int statusCode : statusCodes) {
            if (this.statusCode == statusCode)
                return true;
        }
        return false;
    }

    public Response withMeta(String key, String value) {
        getJson().getNode("meta").put(key, value);
        return this;
    }

    public void write(StringBuilder buff, Object... msgs) {
        write0(buff, msgs);
        buff.append("\r\n");
    }

    protected void write0(StringBuilder buff, Object... msgs) {
        if (msgs != null && msgs.length == 0)
            return;

        if (msgs != null && msgs.length == 1 && msgs[0] != null && msgs[0].getClass().isArray())
            msgs = (Object[]) msgs[0];

        for (int i = 0; msgs != null && i < msgs.length; i++) {
            Object msg = msgs[i];

            if (msg == null)
                continue;

            if (msg instanceof byte[])
                msg = new String((byte[]) msg);

            if (msg.getClass().isArray()) {
                write0(buff, (Object[]) msg);
            } else {
                if (i > 0)
                    buff.append(" ");
                buff.append(msg);
            }

        }
    }

    /**
     * @param status - one of the SC constants ex "200 OK"
     * @return this
     */
    public Response withStatus(String status) {
        statusMesg = status;
        try {
            statusCode = Integer.parseInt(status.substring(0, 3));

            if (statusMesg.length() > 4) {
                statusMesg = status.substring(4);
            }
        } catch (Exception ex) {
            //the status message did not start with numeric status code.
            //this can be ignored.
        }

        return this;
    }

    public String getStatus() {
        return statusCode + " " + statusMesg;
    }

    public Response withStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public Response withStatusMesg(String statusMesg) {
        this.statusMesg = statusMesg;
        return this;
    }

    public Chain getChain() {
        return chain;
    }

    public Engine getEngine() {
        return chain != null ? chain.getEngine() : null;
    }

    public Response withChain(Chain chain) {
        this.chain = chain;
        return this;
    }

    public Response debug(String format, Object... args) {
        write(debug, Utils.format(format, args));
        return this;
    }

    public Response out(Object... msgs) {
        debug(Utils.format(null, msgs));
        write(out, msgs);
        return this;
    }

    public Response withOutput(String output) {
        out = new StringBuilder(output);
        return this;
    }

    public String getOutput() {
        return out.toString();
    }

    public Response dump() {
        System.out.println(getDebug());
        return this;
    }

    public String getDebug() {
        return debug.toString();
    }

    public String getHeader(String key) {
        List<String> vals = headers.get(key);
        if (vals != null && vals.size() > 0)
            return vals.get(0);
        return null;
    }

    /**
     * @return the headers
     */
    public ArrayListValuedHashMap<String, String> getHeaders() {
        return headers;
    }

    public void withHeaders(ArrayListValuedHashMap headers) {
        this.headers.putAll(headers);
    }

    public void withHeader(String key, String value) {
        if (!headers.containsMapping(key, value))
            headers.put(key, value);
    }


    /**
     * Sets the root output json document...you should use withData and withMeta
     * instead unless you REALLY want to change to wrapper document structure.
     *
     * @param json the json to set
     * @return this
     */
    public Response withJson(JSNode json) {
        this.json = json;
        return this;
    }

    public String findString(String path) {
        return getJson().findString(path);
    }

    public int findInt(String path) {
        return getJson().findInt(path);
    }

    public boolean findBoolean(String path) {
        return getJson().findBoolean(path);
    }

    public JSNode findNode(String path) {
        return getJson().findNode(path);
    }

    public JSArray findArray(String path) {
        return getJson().findArray(path);
    }

    public Object find(String path) {
        return getJson().find(path);
    }

    public Validation validate(String jsonPath) {
        return validate(jsonPath, null);
    }

    public Validation validate(String jsonPath, String customErrorMessage) {
        return new Validation(this, jsonPath, customErrorMessage);
    }

    public JSArray data() {
        return getData();
    }

    public JSArray getData() {
        JSNode json = getJson();
        JSArray data = null;
        if (json != null) {
            data = json.getArray("data");
        }
        if(data == null)
            data = json.getArray("_embedded");

        return data;
    }

    public Response withData(JSArray data) {
        String key = getJson().hasProperty("_embedded") ? "_embedded" : "data";
        getJson().put(key, data);
        return this;
    }

    public Response withRecord(Object record) {
        getData().add(record);
        return this;
    }

    public Response withRecords(List records) {
        for (Object record : records)
            getData().add(record);
        return this;
    }

    public JSNode getMeta() {
        return getJson().getNode("meta");
    }

    public Response withMeta(String key, Object value) {
        getMeta().put(key, value);
        return this;
    }

    public Response withFoundRows(int foundRows) {
        withMeta("foundRows", foundRows);
        updatePageCount();
        return this;
    }

    public int getFoundRows() {
        return findInt("meta.foundRows");
    }

    public Response withPageSize(int pageSize) {
        withMeta("pageSize", pageSize);
        return this;
    }

    public Response withPageNum(int pageNum) {
        withMeta("pageNum", pageNum);
        updatePageCount();

        return this;
    }

    public Response withPageCount(int pageCount) {
        withMeta("pageCount", pageCount);
        return this;
    }

    public int getPageNum(){
        return findInt("meta.pageNum");
    }

    public int getPageSize() {
        int pageSize = getJson().findInt("meta.pageSize");
        if (pageSize < 0) {
            Object arr = getJson().find("data.0.name");
            if (arr instanceof JSArray) {
                pageSize = ((JSArray) arr).size();
            }
        }
        return pageSize;
    }

    protected void updatePageCount() {
        int ps = getPageSize();
        int fr = getFoundRows();
        if (ps > 0 && fr > 0) {
            int pageCount = fr / ps + (fr % ps == 0 ? 0 : 1);
            withPageCount(pageCount);
        }
    }

    public int getPageCount() {
        return getJson().findInt("meta.pageCount");
    }

    public String next() {
        return findString("meta.next");
    }

    public Response withNext(String nextPageUrl) {
        withMeta("next", nextPageUrl);
        return this;
    }

    /**
     * @return the statusMesg
     */
    public String getStatusMesg() {
        return statusMesg;
    }

    /**
     * @return the statusCode
     */
    public int getStatusCode() {
        return statusCode;
    }

    public Response withText(String text) {
        this.json = null;
        this.text = text;
        return this;
    }

    public String getRedirect() {
        return redirect;
    }

    public Response withRedirect(String redirect) {
        this.redirect = redirect;
        return this;
    }

    public String getContentType() {
        return contentType;
    }

    public Response withContentType(String contentType) {
        headers.remove("Content-Type");
        headers.put("Content-Type", contentType);
        this.contentType = contentType;
        return this;
    }

    public List<Change> getChanges() {
        return changes;
    }

    public Response withChanges(java.util.Collection<Change> changes) {
        this.changes.addAll(changes);
        return this;
    }

    public Response withChange(String method, String collectionKey, Object resourceKey) {
        if (resourceKey instanceof List) {
            List deletedIds = (List) resourceKey;
            for (Object id : deletedIds) {
                changes.add(new Change(method, collectionKey, id));
            }
        } else {
            changes.add(new Change(method, collectionKey, resourceKey));
        }
        return this;
    }

    public Response withChange(String method, String collectionKey, String... resourceKeys) {
        for (int i = 0; resourceKeys != null && i < resourceKeys.length; i++)
            withChange(method, collectionKey, resourceKeys[i]);
        return this;
    }

    public boolean isSuccess() {
        return statusCode >= 200 && statusCode <= 300 && error == null;
    }

    public Throwable getError() {
        return error;
    }

    public InputStream getInputStream() throws IOException {
        if (file != null)
            return new BufferedInputStream(new FileInputStream(file));

        return null;
    }

    /**
     * @return the json
     */
    public JSNode getJson() {
        if (json == null) {
            //lazy loads text/json
            getContent();
        }

        return json;
    }

    public String getText() {
        if (text == null) {
            //lazy loads text/json
            getContent();
        }

        return text;
    }

    public String getContent() {
        try {
            if (text == null && json == null && file != null && file.length() > 0) {
                String string = Utils.read(getInputStream());
                if (string != null) {
                    try {
                        json = JSNode.parseJsonNode(string);
                    } catch (Exception ex) {
                        //OK
                        text = string;
                    }
                }
            }

            if (text != null) {
                return text;
            } else if (json != null) {
                return json.toString();
            }
        } catch (Exception ex) {
            Utils.rethrow(ex);
        }
        return null;
    }

    public String getErrorContent() {
        if (!isSuccess() && error != null)
            return Utils.getShortCause(error);

        try {
            if (!isSuccess()) {
                String message = findString("message");
                return getStatus() + (!Utils.empty(message) ? " - " + message : "");
            }
        } catch (Exception ex) {
            Utils.rethrow(ex);
        }

        return getStatus();
    }

    public long getFileLength() {
        if (file != null) {
            return file.length();
        }
        return -1;
    }

    public Response withFile(File file) {
        this.json = null;
        this.file = file;
        return this;
    }

    public File getFile() {
        return file;
    }

    /**
     * This is the value returned from the server via the "Content-Length" header
     * NOTE: this will not match file length, for partial downloads, consider also using ContentRangeSize
     *
     * @return the value of the Content-Length header if it exists else 0
     */
    public long getContentLength() {
        String length = getHeader("Content-Length");
        if (length != null) {
            return Long.parseLong(length);
        }
        return 0;
    }

    /**
     * This value come from the "Content-Range" header and is the unit part
     * Content-Range: unit range-start-range-end/size
     *
     * @return the units from the content-range header
     */
    public String getContentRangeUnit() {
        parseContentRange();
        return contentRangeUnit;
    }

    /**
     * This value come from the "Content-Range" header and is the first part
     * Content-Range: unit range-start-range-end/size
     *
     * @return the start value from the content-range header
     */
    public long getContentRangeStart() {
        parseContentRange();
        return contentRangeStart;
    }

    /**
     * This value come from the "Content-Range" header and is the middle part
     * Content-Range: unit range-start-range-end/size
     *
     * @return then end value from the content-range header
     */
    public long getContentRangeEnd() {
        parseContentRange();
        return contentRangeEnd;
    }

    /**
     * This value come from the "Content-Range" header and is the last part
     * Content-Range: unit range-start-range-end/size
     *
     * @return then size value from the content-range header
     */
    public long getContentRangeSize() {
        parseContentRange();
        return contentRangeSize;
    }

    /**
     * Parses the "Content-Range" header
     * Content-Range: <unit> <range-start>-<range-end>/<size>
     */
    private void parseContentRange() {
        if (contentRangeUnit == null) {
            String range = getHeader("Content-Range");
            if (range != null) {
                String[] parts = range.split(" ");
                contentRangeUnit = parts[0];
                parts = parts[1].split("/");
                contentRangeSize = Long.parseLong(parts[1]);
                parts = parts[0].split("-");
                if (parts.length == 2) {
                    contentRangeStart = Long.parseLong(parts[0]);
                    contentRangeEnd = Long.parseLong(parts[1]);
                }
            }
        }
    }

    public Response withUrl(String url) {
        if (!Utils.empty(url)) {
            url = url.trim();
            url = url.replaceAll(" ", "%20");
        }

        this.url = url;

        if (Utils.empty(fileName)) {
            try {
                fileName = new URL(url).getFile();
                if (Utils.empty(fileName))
                    fileName = null;
            } catch (Exception ex) {
                //intentionally blank
            }
        }
        return this;
    }

    public Response withError(Throwable ex) {
        this.error = ex;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public String getUrl() {
        return url;
    }

    public Response withRequest(Request request){
        this.request = request;
        return this;
    }

    public Request getRequest(){
        return request;
    }

    @Override
    public String toString() {
        return debug.toString();
    }

    @Override
    //TODO replace with Cleaner or something similar
    public void finalize() throws Throwable {
        if (file != null) {
            try {
                File tempFile = file;
                file = null;
                tempFile.delete();
            } catch (Throwable t) {
                // ignore
            }
        }
        super.finalize();
    }

    //----------------------------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------------------------
    //TEST ASSERTION CONVENIENCE METHODS

    public Response assertOk(String... messages) {
        if (statusCode < 200 || statusCode > 299) {
            rethrow(statusCode, messages);
        }

        return this;
    }

    public void rethrow() {
        rethrow(statusCode);
    }

    public void rethrow(int statusCode) {
        rethrow(statusCode, (String[]) null);
    }

    public void rethrow(String... messages) {
        rethrow(statusCode, messages);
    }

    public void rethrow(int statusCode, String... messages) {
        if (error != null)
            Utils.rethrow(error);

        statusCode = statusCode > 399 ? statusCode : 500;

        StringBuilder msg = new StringBuilder();
        for (int i = 0; messages != null && i < messages.length; i++)
            msg.append(messages[i]).append("\r\n ");

        String message = getText();
        try {
            while (message != null && message.startsWith("{") && message.contains("\\\"message\\\"")) {
                message = JSNode.parseJsonNode(message).getString("message");
            }
        } catch (Exception ex) {
            //igore
        }

        if (message != null)
            msg.append(" ").append(message.trim());

        throw new ApiException(statusCode + "", null, msg.toString());
    }

    public Response assertStatus(int... statusCodes) {
        return assertStatus(null, statusCodes);
    }

    public Response assertStatus(String message, int... statusCodes) {
        boolean matched = false;
        for (int statusCode : statusCodes) {
            if (statusCode == this.statusCode) {
                matched = true;
                break;
            }
        }

        if (!matched) {
            Object[] args = null;
            if (message == null) {
                message = "The returned status '{}' was not in the approved list '{}'";
                List<Integer> debugList = new ArrayList<>();
                for (int code : statusCodes) debugList.add(code);

                args = new Object[]{this.statusCode, debugList};
            }

            throw ApiException.new500InternalServerError(message, args);
        }

        return this;
    }

    public Response assertDebug(String lineMatch, String... matches) {
        if (matches == null || matches.length == 0) {
            matches = new String[]{lineMatch.substring(lineMatch.indexOf(" ") + 1)};
            lineMatch = lineMatch.substring(0, lineMatch.indexOf(" "));
        }

        String debug = getDebug();

        debug = debug.substring(0, debug.indexOf("<< response"));

        int idx = debug.indexOf(" " + lineMatch + " ");
        if (idx < 0) {
            System.err.println("SKIPPING DEBUG MATCH: " + lineMatch + " " + Arrays.asList(matches));
            return this;
        }

        String debugLine = debug.substring(idx, debug.indexOf("\n", idx)).trim();

        for (String match : matches) {
            List<String> matchTokens = Utils.split(match, ' ', '\'', '"', '{', '}');
            for (String matchToken : matchTokens) {
                if (!debugLine.contains(matchToken)) {
                    String msg = "ERROR: Can't find match token in debug line";
                    msg += "\r\n" + "  - debug line    : " + debugLine;
                    msg += "\r\n" + "  - missing token : " + matchToken;
                    msg += "\r\n" + debug;

                    System.err.println(msg);

                    Utils.error(msg);

                }
            }
        }
        return this;
    }
}