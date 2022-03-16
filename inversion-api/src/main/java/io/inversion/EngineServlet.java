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

import io.inversion.Upload;
import io.inversion.json.JSNode;
import io.inversion.utils.StreamBuffer;
import io.inversion.utils.Utils;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;


public class EngineServlet extends HttpServlet {
    Engine engine = null;//new Engine();

    public static String readBody(HttpServletRequest request) throws ApiException {
        if (request == null)
            return null;

        StringBuilder  stringBuilder  = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                if ("gzip".equalsIgnoreCase(request.getHeader("Content-Encoding")))
                    inputStream = new GZIPInputStream(inputStream, 1024);
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                char[] charBuffer = new char[128];
                int    bytesRead;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            }
        } catch (Exception ex) {
            throw ApiException.new400BadRequest(ex, "Unable to read request body");
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    //throw ex;
                }
            }
        }

        return stringBuilder.toString();
    }

    public void destroy() {
        engine.shutdown();
    }

    public void init(ServletConfig config) {
        engine.startup();
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    @Override
    public void service(HttpServletRequest httpReq, HttpServletResponse httpResp) throws ServletException, IOException {
        EngineServletLocal.set(httpReq, httpResp);

        Response res;
        Request  req;

        try {
            String method = httpReq.getMethod();
            String urlstr = httpReq.getRequestURL().toString();

            if (!urlstr.endsWith("/"))
                urlstr = urlstr + "/";

            String query = httpReq.getQueryString();
            if (!Utils.empty(query)) {
                urlstr += "?" + query;
            }

            String lower = urlstr;
            if (lower.equals("http://localhost")
                    || lower.startsWith("http://localhost/")
                    || lower.startsWith("http://localhost:")
                    || lower.equals("https://localhost")
                    || lower.startsWith("https://localhost/")
                    || lower.startsWith("https://localhost:")
            ) {

                urlstr = Pattern.compile("localhost", Pattern.CASE_INSENSITIVE).matcher(urlstr).replaceFirst("127.0.0.1");
                httpResp.sendRedirect(urlstr);
                return;
            }


            ArrayListValuedHashMap headers    = new ArrayListValuedHashMap<>();
            Enumeration<String>    headerEnum = httpReq.getHeaderNames();
            while (headerEnum.hasMoreElements()) {
                String      key    = headerEnum.nextElement();
                Enumeration values = httpReq.getHeaders(key);
                while (values.hasMoreElements()) {
                    String val = (String) values.nextElement();
                    headers.put(key, val);
                }
            }

            Map<String, String> params       = new HashMap<>();
            Enumeration<String> paramsEnumer = httpReq.getParameterNames();
            while (paramsEnumer.hasMoreElements()) {
                String   key    = paramsEnumer.nextElement();
                String[] values = httpReq.getParameterValues(key);
                String   value  = values == null ? null : (values.length == 1 ? values[0] : Utils.implode(",", values));
                params.put(key, value);
            }

            String body = readBody(httpReq);

            if (body != null && body.startsWith("--") && body.indexOf("Content-Disposition") > 0) {
                throw ApiException.new400BadRequest("Received invalid multipart content.");
            }


            req = new Request(method, urlstr, body, params, headers);
            req.withRemoteAddr(httpReq.getRemoteAddr());

            req.withUploader(() -> {
                try {
                    String      fileName    = null;
                    long        fileSize    = 0;
                    String      requestPath = null;
                    InputStream inputStream = null;

                    for (Part part : httpReq.getParts()) {
                        if (part.getName() == null) {
                            continue;
                        }
                        if (part.getName().equals("file")) {
                            inputStream = part.getInputStream();
                            fileName = part.getSubmittedFileName();
                            fileSize = part.getSize();
                        } else if (part.getName().equals("requestPath")) {
                            requestPath = Utils.read(part.getInputStream());
                            if (Utils.startsWith(requestPath, "/"))
                                requestPath = requestPath.substring(1);
                        }
                    }

                    List uploads = new ArrayList<>();

                    if (inputStream != null) {
                        uploads.add(new Upload(fileName, fileSize, requestPath, inputStream));
                    }
                    return uploads;
                } catch (Exception ex) {
                    Utils.rethrow(ex);
                }
                return null;
            });

            res = new Response();
            engine.service(req, res);
            writeResponse(req, res, httpResp);
        } catch (Throwable ex) {
            JSNode       json  = Engine.buildErrorJson(ex);
            OutputStream out   = httpResp.getOutputStream();
            byte[]       bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            out.write(bytes);
            out.flush();
            out.close();
        }
    }

    void writeResponse(Request req, Response res, HttpServletResponse http) throws Exception {

        http.setStatus(res.getStatusCode());
        OutputStream out = http.getOutputStream();

        ArrayListValuedHashMap<String, String> headers = res.getHeaders();
        headers.keySet().forEach(key -> http.setHeader(key, Utils.implode(",", res.getHeaders().get(key))));

        if (req.isMethod("OPTIONS")) {
            //
        } else {
            String contentType = res.getContentType();
            http.setContentType(contentType);

            StreamBuffer buffer = res.getOutput();
            if (buffer != null) {
                http.setContentLength(buffer.getLength());
                Utils.pipe(buffer.getInputStream(), out, true, false);
            }
        }
        out.flush();
        out.close();
    }

    static class EngineServletLocal {
        static final ThreadLocal<HttpServletRequest>  request  = new ThreadLocal();
        static final ThreadLocal<HttpServletResponse> response = new ThreadLocal();

        public static void set(HttpServletRequest req, HttpServletResponse res) {
            request.set(req);
            response.set(res);
        }

        public static HttpServletRequest getRequest() {
            return request.get();
        }

        public static void setRequest(HttpServletRequest req) {
            request.set(req);
        }

        public static HttpServletResponse getResponse() {
            return response.get();
        }

        public static void setResponse(HttpServletResponse res) {
            response.set(res);
        }
    }
}
