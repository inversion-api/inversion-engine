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

import io.inversion.utils.Path;
import io.inversion.utils.Task;
import io.inversion.utils.Utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Actions perform some work when matched to a Request and potentially contribute to the content of the Response.
 *
 * <p><b>The Action Sandwich</b>
 * <p>
 * Nearly the entire job of the Engine and Api Endpoint configuration is to match one or more Action subclass instances to a Request.
 * All matched actions are sorted by their <code>order</code> property and executed in sequence as a Chain of Responsibility pattern.
 * Colloquially, at Rocket Partners we somehow started to call this the 'action sandwich'.
 * <p>
 * If an Action should be run across multiple Endpoints, a logging or security Action for example, it can be added directly to an Api via Api.withAction.
 * <p>
 * In most cases however, you will group sets of related actions under an Endpoint and add the Endpoint to the Api.
 * <p>
 * Both Api registered Actions and Endpoint registered Actions can be selected into the 'action sandwich' for a particular Request.
 * <p>
 * One big difference however is that <code>includesPaths</code> and <code>excludePaths</code> are relative to the Api path for
 * <p>
 * Api registered Actions and relative to the Endpoint path for Endpoint registered actions.  They are all sorted togeter in one big group according to order.
 *
 *
 * <h2>Handling Requests</h2>
 * <p>
 * Once the Engine has selected the Actions to run for a given request (creating the 'action sandwich'), they are all loaded into a Chain object
 * which is responsible for invoking {@link #run(Request, Response)} on each one in sequence.
 * <p>
 * You can override <code>run</code> to process all Requests this Action is selected for, or you can override any of the HTTP method specific doGet/Post/Put/Patch/Delete() handlers
 * if you want to segregate your business logic by HTTP method.
 */
public class Action<A extends Action> extends Rule<A> {

    boolean decoration = false;

    public Action() {

    }

    public List<Path> getFullIncludePaths(Api api, Db db, String method, Path endpointPath, boolean relative) {
        endpointPath = new Path(endpointPath);

        Path base = new Path();
        if (relative) {
            while (endpointPath.size() > 0 && !endpointPath.isOptional(0) && !endpointPath.isWildcard(0))
                base.add(endpointPath.remove(0));
        }

        LinkedHashSet<Path> fullPaths        = new LinkedHashSet();
        List<Path>          endpointSubPaths = endpointPath.getSubPaths();
        for (Path endpointSubPath : endpointSubPaths) {
            for (Path actionSubPath : getIncludePaths(api, db, method)) {
                Path fullPath = joinPaths(endpointSubPath, actionSubPath, relative);
                if (fullPath != null) {
                    fullPaths.add(new Path(base.toString(), fullPath.toString()));
                }
            }
        }
        List<Path> returnPaths = new ArrayList(fullPaths);
        return returnPaths;
    }

    protected LinkedHashSet<Path> getIncludePaths(Api api, Db db, String method) {
        LinkedHashSet<Path> includePaths = new LinkedHashSet<>();
        for (RuleMatcher matcher : getIncludeMatchers()) {
            if (matcher.hasMethod(method)) {
                for (Path actionPath : matcher.getPaths()) {
                    includePaths.addAll(actionPath.getSubPaths());
                }
            }
        }
        return includePaths;
    }

    public static Path joinPaths(Path endpointPath, Path actionPath, boolean relative) {
        Path val = joinPaths0(endpointPath, actionPath, relative);
        //System.out.println("joinPaths(" + endpointPath + ", " + actionPath + ", " + relative + ") -> " + val);
        return val;
    }

    public static Path joinPaths0(Path endpointPath, Path actionPath, boolean relative) {

        endpointPath = new Path(endpointPath);
        actionPath = new Path(actionPath);


        Path merged = new Path();

        while (true) {
            if (endpointPath.isWildcard()) {
                merged = new Path(merged.toString(), actionPath.toString());
                break;
            }
            if (actionPath.isWildcard()) {
                merged = new Path(merged.toString(), endpointPath.toString());
                break;
            }

            if(endpointPath.size() == 0 || actionPath.size() == 0){
                if(endpointPath.size() == actionPath.size())
                    break;
                else
                    return null;
            }

            if ((endpointPath.size() == 0 || actionPath.size() == 0) && actionPath.size() != endpointPath.size())
                return null;

            boolean epVar = endpointPath.isVar(0);
            boolean aVar  = actionPath.isVar(0);

            String epVal = Path.unwrapOptional(endpointPath.remove(0));
            String aVal  = Path.unwrapOptional(actionPath.remove(0));

            if (!epVar && !aVar) {
                if (!epVal.equalsIgnoreCase(aVal))
                    return null;
                else
                    merged.add(epVal);
            } else if (!aVar) {
                merged.add(aVal);
            } else {
                merged.add(epVal);
            }
        }

        return merged;
    }

    /**
     * This task has been selected to run as part of the supplied operation, this
     * callback allows actions to perform any custom configuration on the op.
     *
     * @param task
     * @param op
     */
    public void configureOp(Task task, Op op) {
        getParams().forEach(p -> op.withParam(p));
    }


    /**
     * Override this method with your custom business logic or override one of the
     * http method "doMETHOD" specific handlers.
     *
     * @param req the Request being serviced
     * @param res the Reponse being generated
     */
    public void run(Request req, Response res) throws ApiException {
        run0(req, res);
    }

    protected void run0(Request req, Response res) throws ApiException {

        String collectionKey = req.getCollectionKey();
        String methodKey     = req.getUrl().getParam("_method");

        Method method = null;
        if (methodKey != null)
            method = Utils.getMethod(getClass(), methodKey);
        if (method == null && collectionKey != null)
            method = Utils.getMethod(getClass(), "do" + collectionKey + req.getMethod());
        if (method == null)
            method = Utils.getMethod(getClass(), "do" + collectionKey);
        if (method == null)
            method = Utils.getMethod(getClass(), "do" + req.getMethod());

        if (method != null) {
            try {
                method.invoke(this, req, res);
            } catch (Throwable ex) {
                if (!(ex instanceof ApiException))
                    ex = ex.getCause();

                ex.printStackTrace();

                if (!(ex instanceof ApiException))
                    ex = ApiException.new500InternalServerError(ex);

                throw (ApiException) ex;
            }
        }
    }

    /**
     * Handle an HTTP GET.
     * <p>
     * Override run() to handle all requests or override this method with your business logic specifically for a GET request
     *
     * @param req the request to run
     * @param res the response to populate
     */
    public void doGet(Request req, Response res) throws ApiException {

    }

    /**
     * Handle an HTTP POST.
     * <p>
     * Override run() to handle all requests or override this method with your business logic for a POST request
     *
     * @param req the request to run
     * @param res the response to populate
     */

    public void doPost(Request req, Response res) throws ApiException {

    }

    /**
     * Handle an HTTP PUT.
     * <p>
     * Override run() to handle all requests or override this method with your business logic for a PUT request
     *
     * @param req the request to run
     * @param res the response to populate
     */
    public void doPut(Request req, Response res) throws ApiException {

    }

    /**
     * Handle an HTTP PATCH.
     * <p>
     * Override run() to handle all requests or override this method with your business logic for a PATCH request
     *
     * @param req the request to run
     * @param res the response to populate
     */
    public void doPatch(Request req, Response res) throws ApiException {

    }

    /**
     * Handle an HTTP DELETE.
     * <p>
     * Override run() to handle all requests or override this method with your business logic for a DELETE request
     *
     * @param req the request to run
     * @param res the response to populate
     */
    public void doDelete(Request req, Response res) throws ApiException {

    }

    public boolean isDecoration() {
        return decoration;
    }

    public A withDecoration(boolean decoration) {
        this.decoration = decoration;
        return (A) this;
    }
}
