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
import io.inversion.utils.Utils;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.*;

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
     */
    protected boolean internal = false;

    public Endpoint() {

    }

    public Endpoint(String methods, String includePaths, Action... actions) {
        withIncludeOn(methods, includePaths);

        if (actions != null) {
            for (Action action : actions)
                withAction(action);
        }
    }

    Operation buildOperation(Api api, String method, Path apiPath, Path epPath, List<Action> apiActions){

        List<Action> actions = new ArrayList(this.actions);
        actions.addAll(apiActions);

        Collections.sort(actions);

        Operation op = new Operation();

        op.withApi(api);
        op.withEndpoint(this);
        op.withMethod(method);
        op.withApiMatchPath(apiPath);
        op.withEpMatchPath(epPath);

        for(Action action : actions){
            action.buildOperation(op);
        }

        return op;
    }


    List<Path> getOperationPaths(Api api, String method) {
        List<Path> mergedPaths = new ArrayList();
        for (Rule.RuleMatcher epMatcher : getIncludeMatchers()) {
            if(epMatcher.hasMethod(method)){
                boolean added = false;
                for(Path epPath : epMatcher.getPaths()){
                    for(Action action : actions){
                        Path epPathCopy = new Path(epPath);
                        List<Path> actionPaths = action.getOperationPaths(api, this, method, epPathCopy);
                        if(mergedPaths != null && mergedPaths.size() > 0){
                            mergePaths(mergedPaths, actionPaths);
                        }
                    }
                }
            }
        }
        return mergedPaths;
    }



//    ArrayListValuedHashMap<String, Path> getOperationPaths(Api api, Path apiPath) {
//        ArrayListValuedHashMap<String, Path> opPaths = new ArrayListValuedHashMap<>();
//
////        ArrayListValuedHashMap<String, Path> mergedPaths = new ArrayListValuedHashMap();
////        for (Action action : actions) {
////            ArrayListValuedHashMap<String, Path> actionPaths = action.getOperationPaths(api);
////            mergePaths(mergedPaths, actionPaths);
////        }
//
//
//        Map<Action, ArrayListValuedHashMap<String, Path>> actionOpPaths = new HashMap();
//
//        for (Action action : actions) {
//            ArrayListValuedHashMap<String, Path> paths = action.getOperationPaths(api);
//            actionOpPaths.put(action, paths);
//        }
//
//
//        for (Rule.RuleMatcher epMatcher : getIncludeMatchers()) {
//            for (String method : epMatcher.getMethods()) {
//                List<Path> paths = epMatcher.getPaths();
//                for (Path path : paths) {
//
//                    Path actionMatchPath = path.getOptionalSuffix();
//
//                    System.out.println(path + " - " + actionMatchPath);
//
//                    List<Path> mergedPaths = new ArrayList();
//                    boolean    hasMatches  = false;
//                    for (Action action : actions) {
//                        if (action.matches(method, actionMatchPath)) {
//                            List<Path> actionPaths = actionOpPaths.get(action).get(method);
//                            mergePaths(mergedPaths, actionPaths);
//                            hasMatches = true;
//                        }
//                    }
//
//                    if (hasMatches) {
//                        Path opPath = new Path(path.getRequiredPrefix());
//                        if (opPath.size() > 0 && opPath.endsWithWildcard())
//                            opPath.removeTrailingWildcard();
//
//                        if (mergedPaths.size() > 0) {
//                            for (Path actionPath : mergedPaths) {
//                                opPaths.put(method, new Path(opPath).add(actionPath.toString()));
//                            }
//                        } else {
//
//                            List<Path> subPaths = path.getSubPaths();
//                            for(int i=0; i<subPaths.size(); i++){
//                                Path sp = subPaths.get(i);
//                                if(i == subPaths.size()-1){
//                                    if(path.endsWithWildcard() && !sp.endsWithWildcard())
//                                        sp.add("*");
//                                }
//                                opPaths.put(method, sp);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        return opPaths;
//
//    }

    /**
     * DbAction gives
     * /books
     * /books/${bookId}
     * /books/${bookId}/author
     * <p>
     * /authors
     * /authors/${authorId}
     * /authors/${authorId}/books
     * <p>
     * Something else gives:
     * /${planes}/bob/sue
     * <p>
     * Something else gives:
     * /*
     * <p>
     * Something else gives:
     * <p>
     * a/{asd}
     * a/{zxcvzxc}
     * {asdf}/b
     */
    static void mergePaths(List<Path> mPaths, List<Path> aPaths) {

        for (Path aPath : aPaths) {

            boolean exists = false;
            for (int m= 0; m<mPaths.size(); m++){

                Path mPath = mPaths.get(m);
                if (aPath == mPath)
                    continue;

                for (int i = 0; i < aPath.size(); i++) {

                    if (i >= mPath.size()) {
                        break;
                    }

                    //the old rule consumes the new one  a/b consumes a/*
                    if(aPath.isWildcard(i) && !mPath.isWildcard(i)){
                        exists = true;
                        break;
                    }

                    //the new rule will consume the old one....a/* consumed by a/b or a/b/c
                    if(mPath.isWildcard(i)){
                        mPaths.remove(m);
                        m--;
                        break;
                    }

                    if (!mPath.isVar(i) && aPath.isVar(i))
                        aPath.set(i, mPath.get(i));

                    if (mPath.isVar(i) && !aPath.isVar(i)){
                        mPath.set(i, aPath.get(i));
                    }

                    if (!mPath.isVar(i) && !aPath.isVar(i)) {
                        if (!mPath.get(i).equalsIgnoreCase(aPath.get(i)))
                            throw ApiException.new500InternalServerError("Endpoint configuration error.  Actions have incompatible paths.");
                    }

                    if(aPath.size() == mPath.size() && i==mPath.size()-1)
                        exists = true;
                }
            }

            if (!exists) {
                mPaths.add(aPath);
            }
        }

        //-- removes "a" if "a/*" is in the list.
        for(int i=0;i<mPaths.size(); i++){
            for(int j=0;j<mPaths.size(); j++){
                if(i==j)
                    continue;

                Path p1 = mPaths.get(i);
                Path p2 = mPaths.get(j);

                if(p1.size() == p2.size()+1 && p1.endsWithWildcard()){
                    mPaths.remove(j);
                    j--;
                    continue;
                }
                if(p2.size() == p1.size()+1 && p2.endsWithWildcard()){
                    mPaths.remove(i);
                    i--;
                    break;
                }
            }
        }

        Collections.sort(mPaths, new Comparator<Path>() {
            @Override
            public int compare(Path o1, Path o2) {
                if(o1.size() < o2.size())
                    return -1;
                if(o1.size() > o2.size())
                    return 1;
                return o1.toString().compareTo(o2.toString());
            }
        });

    }


    public Endpoint withInternal(boolean internal) {
        this.internal = internal;
        return this;
    }

    public boolean isInternal() {
        return internal;
    }

    public List<Action> getActions() {
        return new ArrayList(actions);
    }

    public Endpoint withActions(Action... actions) {
        for (Action action : actions)
            withAction(action);

        return this;
    }

    public Endpoint withAction(Action action) {
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
