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

import io.inversion.json.*;
import io.inversion.utils.MimeTypes;
import io.inversion.utils.StreamBuffer;
import io.inversion.utils.Utils;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class serves as holder for the Response returned from a RestClient call AND as the object
 * used to construct your own response to an Engine request.
 */
public class Response implements JSFind {

    protected Chain   chain   = null;
    protected Request request = null;

    protected int    statusCode = 200;
    protected String statusMesg = "OK";

    protected String url      = null;
    protected String fileName = null;

    protected final ArrayListValuedHashMap<String, String> headers = new ArrayListValuedHashMap<>();

    protected JSNode       json   = new JSMap("meta", new JSMap(), "data", new JSList());
    protected String       text   = null;
    protected StreamBuffer stream = null;

    protected Throwable error = null;

    protected final StringBuilder debug   = new StringBuilder();
    protected final List<Change>  changes = new ArrayList<>();

    protected long startAt = System.currentTimeMillis();
    protected long endAt   = -1;

    public Response() {

    }

    public Response(String url) {
        withUrl(url);
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
        this.text = null;
        this.stream = null;
        return this;
    }

    public Response withJson(String json) {
        if (json == null)
            withJson((JSNode) null);
        else
            withJson((JSNode) JSParser.parseJson(json));
        return this;
    }

    public Response withText(String text) {
        this.text = text;
        this.json = null;
        this.stream = null;
        return this;
    }

    public Response withStream(StreamBuffer stream, String fileName) {
        withStream(stream);
        withFileName(fileName);
        return this;
    }

    public Response withStream(StreamBuffer stream) {
        this.text = null;
        this.json = null;
        this.stream = stream;
        return this;
    }

    public JSNode getJson() {
        if (stream != null) {

            if (!MimeTypes.TYPE_APPLICATION_JSON.equalsIgnoreCase(getContentType()))
                return null;

            try {
                json = (JSNode) JSParser.parseJson(stream.getInputStream());
            } catch (Exception e) {
                throw new ApiException(e);
            } finally {
                stream = null;
            }
        }

        return json;
    }

    public String getText() {
        if (stream != null) {
            try {
                text = Utils.read(stream.getInputStream());
            } catch (Exception e) {
                throw new ApiException(e);
            } finally {
                stream = null;
            }
        }
        return text;
    }

    public StreamBuffer getOutput() {
        boolean explain = false;
        Request req     = getRequest();
        if (req == null && Chain.getDepth() > 0)
            req = Chain.peek().getRequest();

        if (req != null)
            explain = req.isDebug() && req.isExplain();

        return getOutput(explain);
    }

    public StreamBuffer getOutput(boolean explain) {

        StreamBuffer output = stream;
        try {
            if (output == null) {

                if (json != null) {
                    output = new StreamBuffer();
                    output.write(json.toString().getBytes(StandardCharsets.UTF_8));
                } else if (text != null) {
                    output = new StreamBuffer();
                    output.write(text.getBytes(StandardCharsets.UTF_8));
                }
            }

            if (explain) {
                StringBuilder buff = new StringBuilder("");
                buff.append(debug.toString());
                if (error != null) {
                    buff.append("\r\n<< error ----------------");
                    buff.append("\r\n").append(Utils.getShortCause(error));
                }
                buff.append("\r\n<< response -------------");
                buff.append("\r\n").append(getStatus());
                buff.append("\r\n");
                for (String key : getHeaders().keySet()) {
                    buff.append("\r\n").append(key).append(" ").append(getHeader(key));
                }
                buff.append("\r\n");

                if (output != null) {
                    String text = Utils.read(output.getInputStream());
                    buff.append(text);
                }
                output = new StreamBuffer();
                output.write(buff.toString().getBytes(StandardCharsets.UTF_8));
            }

        } catch (IOException e) {
            throw new ApiException(e);
        }
        return output;
    }

    public Response debug(String format, Object... args) {
        debug.append(Utils.format(format, args)).append("\r\n");
        return this;
    }

    public String getDebug() {
        try {
            return Utils.read(getOutput(true).getInputStream());
        } catch (IOException ex) {
            throw new ApiException(ex);
        }
    }

    public Response dump() {
        System.out.println(getDebug());
        return this;
    }


    public long getStartAt() {
        return startAt;
    }

    public Response withStartAt(long startAt) {
        this.startAt = startAt;
        return this;
    }

    public long getEndAt() {
        return endAt;
    }

    public Response withEndAt(long endAt) {
        this.endAt = endAt;
        return this;
    }

    public Response withUrl(String url) {
        if (!Utils.empty(url)) {
            url = url.trim();
            url = url.replaceAll(" ", "%20");
        }

        this.url = url;

        if (Utils.empty(fileName)) {
            try {
                String fileName = new URL(url).getFile();
                withFileName(fileName);
            } catch (Exception ex) {
                //intentionally blank
            }
        }
        return this;
    }

    public Response withFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getUrl() {
        if (url == null && request != null)
            return request.getUrl().toString();
        return url;
    }

    public Response withRequest(Request request) {
        this.request = request;
        return this;
    }

    public Request getRequest() {
        return request;
    }

    public Op getOp() {
        return request != null ? request.getOp() : null;
    }

    public Chain getChain() {
        if (chain == null && request != null)
            return request.getChain();
        return chain;
    }

    public Response withChain(Chain chain) {
        this.chain = chain;
        return this;
    }

    public Engine getEngine() {
        return getChain() != null ? getChain().getEngine() : null;
    }

    @Override
    public String toString() {
        return debug.toString();
    }


    public Response withError(Throwable ex) {
        this.error = ex;
        return this;
    }

    public Throwable getError() {
        return error;
    }

    //----------------------------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------------------------
    //Meta Construction

    public Response withMeta(String key, Object value) {
        JSNode json = getJson();
        JSNode meta = json.getMap("meta");
        if (meta == null)
            meta = json;
        meta.putValue(key, value);
        return this;
    }

    public JSNode getMeta() {
        JSNode json = getJson();
        JSNode meta = json.getMap("meta");
        if (meta == null)
            meta = json;
        return meta;
    }

    public Response withFoundRows(int foundRows) {
        withMeta("foundRows", foundRows);
        updatePageCount();
        return this;
    }

    public int getFoundRows() {
        return getMeta().findInt("foundRows");
    }

    public Response withLastKey(String lastKey) {
        withMeta("lastKey", lastKey);
        return this;
    }

    public String getLastKey() {
        return getMeta().findString("lastKey");
    }


    public Response withPageSize(int pageSize) {
        withMeta("pageSize", pageSize);
        return this;
    }

    public int getPageSize() {
        return getMeta().findInt("pageSize");
    }

    public Response withPageNum(int pageNum) {
        withMeta("pageNum", pageNum);
        updatePageCount();
        return this;
    }

    public int getPageNum() {
        return getMeta().findInt("pageNum");
    }

    public Response withPageCount(int pageCount) {
        withMeta("pageCount", pageCount);
        return this;
    }

    public int getPageCount() {
        return getMeta().findInt("pageCount");
    }

    protected void updatePageCount() {
        int ps = getPageSize();
        int fr = getFoundRows();
        if (ps > 0 && fr > 0) {
            int pageCount = fr / ps + (fr % ps == 0 ? 0 : 1);
            withPageCount(pageCount);
        }
    }

    public Response withLink(String name, String url) {
        JSNode links = getJson().findMap("_links");
        if (links != null) {
            links.putValue(name, new JSMap("href", url));
        } else {
            getMeta().putValue(name, url);
        }
        return this;
    }

    public String getLink(String name) {
        JSNode links = getJson().findMap("_links");
        if (links != null) {
            JSNode link = links.getNode(name);
            if (link != null)
                return link.getString("href");
        } else {
            Object link = getMeta().getValue(name);
            if (link instanceof String)
                return (String) link;
        }
        return null;
    }

    public String getSelf() {
        return getLink("self");
    }

    public Response withSelf(String url) {
        withLink("self", url);
        return this;
    }

    public String getNext() {
        return getLink("next");
    }

    public Response withNext(String url) {
        withLink("next", url);
        return this;
    }

    public String getPrev() {
        return getLink("prev");
    }

    public Response withPrev(String url) {
        withLink("prev", url);
        return this;
    }

    public String getFirst() {
        return getLink("first");
    }

    public Response withFirst(String url) {
        withLink("first", url);
        return this;
    }

    public String getLast() {
        return getLink("last");
    }

    public Response withLast(String url) {
        withLink("last", url);
        return this;
    }


    //----------------------------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------------------------
    //Data Construction

    public JSList data() {
        JSNode json = getJson();

        if (json == null)
            return null;

        if (json instanceof JSList)
            return (JSList) json;

        if (json.getValue("data") instanceof JSList)
            return json.getList("data");

        if (json.getValue("_embedded") instanceof JSList)
            return json.getList("_embedded");

        if (json.getValue("items") instanceof JSList)
            return json.getList("items");

        //-- there is a single object in the payload without a meta or payload section
        //-- this could mess up some callers that try to add to the returned
        //-- JSList instead of calling withRecord
        return new JSList(json);
    }

    public JSMap getFirstRecordAsMap() {
        JSList data = data();
        if (data == null || data.size() == 0)
            return null;
        return data.getMap(0);
    }

    public Response withRecord(Object record) {
        JSList data = data();
        if (data == null) {
            data = new JSList();
            getJson().putValue("data", data);
        }
        data.add(record);
        return this;
    }

    public Response withRecords(List records) {
        for (Object record : records)
            withRecord(record);
        return this;
    }

    //----------------------------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------------------------
    //Status & Headers


    public boolean isSuccess() {
        return statusCode >= 200 && statusCode <= 300 && error == null;
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

    public boolean hasStatus(int... statusCodes) {
        for (int statusCode : statusCodes) {
            if (this.statusCode == statusCode)
                return true;
        }
        return false;
    }

    public String getStatus() {
        return statusCode + " " + statusMesg;
    }

    public Response withStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    /**
     * @return the statusCode
     */
    public int getStatusCode() {
        return statusCode;
    }


    /**
     * @return the statusMesg
     */
    public String getStatusMesg() {
        return statusMesg;
    }


    public Response withStatusMesg(String statusMesg) {
        this.statusMesg = statusMesg;
        return this;
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

    public String getHeader(String key) {
        List<String> vals = headers.get(key);
        if (vals != null && vals.size() > 0)
            return Utils.implode(",", vals);
        return null;
    }

    public void withHeader(String key, String value) {
        if (!headers.containsMapping(key, value))
            headers.put(key, value);
    }

    public String getRedirect() {
        return getHeader("Location");
    }

    public Response withRedirect(String redirect) {
        if (redirect == null) {
            headers.remove("Location");
            if (308 == getStatusCode())
                withStatus(Status.SC_200_OK);
        } else {
            withHeader("Location", redirect);
            withStatus(Status.SC_308_PERMANENT_REDIRECT);
        }
        return this;
    }

    public Response withContentType(String contentType) {
        headers.remove("Content-Type");
        withHeader("Content-Type", contentType);
        return this;
    }

    public String getContentType() {
        String contentType = getHeader("Content-Type");
        if (contentType == null) {

            if (json != null)
                return MimeTypes.TYPE_APPLICATION_JSON;

            if (fileName != null) {
                int dot = fileName.lastIndexOf('.');
                if (dot > 0) {
                    String ext = fileName.substring(dot + 1);
                    contentType = MimeTypes.getMimeType(ext);
                }
            }
        }
        return contentType;
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

    //----------------------------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------------------------
    //Changes


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
                message = JSParser.asJSNode(message).getString("message");
            }
        } catch (Exception ex) {
            //igore
        }

        if (message != null)
            msg.append(" ").append(message.trim());

        throw new ApiException(statusCode + "", (msg != null ? msg.toString() : null));
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