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

import java.util.ArrayList;
import java.util.List;

/**
 * A single Endpoint, bundling one or more Path match relative Actions, is selected to service a Request.
 */
public class Endpoint extends Rule<Endpoint> {
    /**
     * The Actions that are 'local' to this request.
     * <p>
     * These Actions are not run each Request automatically.
     * <p>
     * Compared to Actions that are registered directly with the Api via Api.withAction, these Actions are
     * path matched relative to the Path that matched to select this Endpoint vs a different Endpoint.
     */
    protected final List<Action> actions = new ArrayList<>();

    /**
     * Internal Endpoints can only be called by recursive calls to the engine when Chain.depth() is @gt; 1.
     * This could have been logically called 'private' but that is a reserved word in Java.
     */
    protected boolean internal = false;

    protected transient Api api = null;

    public Endpoint() {

    }

    public Endpoint(Action... actions) {
        if (actions != null) {
            for (Action action : actions)
                withAction(action);
        }
    }

    public Endpoint(String ruleMatcherSpec, Action... actions) {
        withIncludeOn(ruleMatcherSpec);

        if (actions != null) {
            for (Action action : actions)
                withAction(action);
        }
    }

    public Api getApi() {
        return api;
    }

    public Endpoint withApi(Api api) {
        this.api = api;
        api.withEndpoint(this);
        return this;
    }

    public Endpoint withInternal(boolean internal) {
        this.internal = internal;
        return this;
    }

    public boolean isInternal() {
        return internal;
    }

    public Action getAction(String name){
        for(Action action : actions){
            if(name.equalsIgnoreCase(action.getName()))
                return action;
        }
        return null;
    }

    public List<Action> getActions() {
        return new ArrayList(actions);
    }

    public Endpoint withActions(Action... actions) {
        for (int i =0; actions != null && i<actions.length; i++)
            withAction(actions[i]);

        return this;
    }

    public Endpoint withAction(Action action) {

        if(action == null)
            return this;

        if (actions.contains(action))
            return this;

        boolean inserted = false;
        for (int i = 0; i < actions.size(); i++) {
            if (action.getOrder() < actions.get(i).getOrder()) {
                actions.add(i, action);
                inserted = true;
                break;
            }
        }

        if (!inserted)
            actions.add(action);

        return this;
    }

}
