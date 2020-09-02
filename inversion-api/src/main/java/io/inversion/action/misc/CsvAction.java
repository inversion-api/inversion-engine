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

import io.inversion.Action;
import io.inversion.ApiException;
import io.inversion.Request;
import io.inversion.Response;
import io.inversion.utils.JSArray;
import io.inversion.utils.JSNode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Converts a JSON object/array response value into CSV format.
 * <p>
 * Works for a status code 200 GET request when 'format=csv' is passed in on the query string
 * or the Endpoint or an action has 'format=csv' as part of its config.
 */
public class CsvAction extends Action<CsvAction> {
    @Override
    public void run(Request req, Response res) throws ApiException {
        if (!"GET".equals(req.getMethod()) || 200 != res.getStatusCode() || res.getJson() == null || res.getText() != null) {
            return;
        }

        if (!"csv".equalsIgnoreCase(req.getUrl().getParam("format")) && !"csv".equalsIgnoreCase(req.getChain().getConfig("format", null))) {
            return;
        }

        //support result being an array, a single object, or an inversion state object where we will pull results from the data field.
        JSNode arr = res.getJson();
        if (!(arr instanceof JSArray)) {
            if (res.getJson().hasProperty("data")) {
                arr = res.getJson().getArray("data");
            } else {
                arr = new JSArray(arr);
            }
        }

        byte[] bytes = toCsv((JSArray) arr).getBytes();

        res.withHeader("Content-Length", bytes.length + "");
        res.debug("Content-Length " + bytes.length + "");
        //res.setContentType("text/csv");

        res.withText(new String(bytes));
    }

    public String toCsv(JSArray arr) throws ApiException {
        try {
            StringBuilder buff = new StringBuilder();
            LinkedHashSet<String> keys = new LinkedHashSet<>();

            for (int i = 0; i < arr.length(); i++) {
                JSNode obj = (JSNode) arr.get(i);
                if (obj != null) {
                    for (String key : obj.keySet()) {
                        Object val = obj.get(key);
                        if (!(val instanceof JSArray) && !(val instanceof JSNode))
                            keys.add(key);
                    }
                }
            }

            CSVPrinter printer = new CSVPrinter(buff, CSVFormat.DEFAULT);

            List<String> keysList = new ArrayList<>(keys);
            for (String key : keysList) {
                printer.print(key);
            }
            printer.println();

            for (int i = 0; i < arr.length(); i++) {
                for (String key : keysList) {
                    Object val = ((JSNode) arr.get(i)).get(key);
                    if (val != null) {
                        printer.print(val);
                    } else {
                        printer.print("");
                    }
                }
                printer.println();
            }
            printer.flush();
            printer.close();

            return buff.toString();
        } catch (Exception ex) {
            throw ApiException.new500InternalServerError(ex);
        }
    }
}
