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

/**
 * Actions perform some work when matched to a Request and potentially contribute to the content of the Response.
 * <p>
 * <h3>The Action Sandwich</h3>
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
 * <h3>Handling Requests</h3>
 * <p>
 * Once the Engine has selected the Actions to run for a given request (creating the 'action sandwich'), they are all loaded into a Chain object
 * which is responsible for invoking {@link #run(Request, Response)} on each one in sequence.
 * <p>
 * You can override <code>run</code> to process all Requests this Action is selected for, or you can override any of the HTTP method specific doGet/Post/Put/Patch/Delete() handlers
 * if you want to segregate your business logic by HTTP method.
 */
public abstract class Action<A extends Action> extends Rule<A> {
    public Action() {

    }

    public Action(String methods, String... includePaths) {
        withIncludeOn(methods, includePaths);
    }

    /**
     * Override this method with your custom business logic or override one of the
     * http method "doMETHOD" specific handlers.
     *
     * @param req the Request being serviced
     * @param res the Reponse being generated
     * @throws ApiException
     */
    public void run(Request req, Response res) throws ApiException {
        if (req.isGet())
            doGet(req, res);
        else if (req.isPost())
            doPost(req, res);
        else if (req.isPut())
            doPut(req, res);
        else if (req.isPatch())
            doPatch(req, res);
        else if (req.isDebug())
            doDelete(req, res);
    }

    /**
     * Handle an HTTP GET.
     * <p>
     * Override run() or override this method with your business logic, otherwise a 501 will be thrown if it is called.
     *
     * @param req
     * @param res
     * @throws ApiException
     */
    public void doGet(Request req, Response res) throws ApiException {
        ApiException.throw501NotImplemented("Either exclude GET requests for this Action in your Api configuration or override run() or doGet().");
    }

    /**
     * Handle an HTTP POST.
     * <p>
     * Override run() or override this method with your business logic, otherwise a 501 will be thrown if it is called.
     *
     * @param req
     * @param res
     * @throws ApiException
     */

    public void doPost(Request req, Response res) throws ApiException {
        ApiException.throw501NotImplemented("Either exclude POST requests for this Action in your Api configuration or override run() or doPost().");
    }

    /**
     * Handle an HTTP PUT.
     * <p>
     * Override run() or override this method with your business logic, otherwise a 501 will be thrown if it is called.
     *
     * @param req
     * @param res
     * @throws ApiException
     */
    public void doPut(Request req, Response res) throws ApiException {
        ApiException.throw501NotImplemented("Either exclude PUT requests for this Action in your Api configuration or override run() or doPut().");
    }

    /**
     * Handle an HTTP PATCH.
     * <p>
     * Override run() or override this method with your business logic, otherwise a 501 will be thrown if it is called.
     *
     * @param req
     * @param res
     * @throws ApiException
     */
    public void doPatch(Request req, Response res) throws ApiException {
        ApiException.throw501NotImplemented("Either exclude PATCH requests for this Action in your Api configuration or override run() or doPatch().");
    }

    /**
     * Handle an HTTP DELETE.
     * <p>
     * Override run() or override this method with your business logic, otherwise a 501 will be thrown if it is called.
     *
     * @param req
     * @param res
     * @throws ApiException
     */
    public void doDelete(Request req, Response res) throws ApiException {
        ApiException.throw501NotImplemented("Either exclude DELETE requests for this Action in your Api configuration or override run() or doDelete().");
    }
}
