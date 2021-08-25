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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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


    List<Operation> generateOperations(Operation apiOp) {
        return null;
    }

//    List<Operation> generateOperations(Operation apiOp) {
//        List<Operation> operations = new ArrayList<>();
//        for (Rule.RuleMatcher epMatcher : getIncludeMatchers()) {
//            if (!epMatcher.hasMethod(apiOp.getMethod()))
//                continue;
//
//            for (Path epPath : epMatcher.getPaths()) {
//                Path fullEpPath = Path.joinPaths(apiOp.apiMatchPath, epPath);
//                if (fullEpPath == null)
//                    continue;
//
//                apiOp.withEndpoint(this);
//                apiOp.withEpMatchPath(epPath);
//
//                List<Action> actions = new ArrayList(this.actions);
//                for (Action apiAction : apiOp.getApi().getActions()) {
//                    if (!actions.contains(apiAction)) {
//                        actions.add(apiAction);
//                    }
//                }
//                Collections.sort(actions);
//
//                List<Path> actionPaths = new ArrayList();
//                for (Action action : actions) {
//                    for (Rule.RuleMatcher actionMatcher : (List<Rule.RuleMatcher>) action.getIncludeMatchers()) {
//                        if(!actionMatcher.hasMethod(apiOp.getMethod()))
//                            continue;
//
//                        for(Path actionPath : actionMatcher.getPaths()){
//                            if(this.actions.contains(action)){
//                                Path fullActionPath = Path.joinPaths(apiOp.epMatchPath, actionPath);
//                                if(fullActionPath != null) {
//                                    actionPaths.add(fullActionPath);
//                                }
//                            }
//                            else{
//                                if(actionPath.matches(apiOp.epMatchPath)){
//                                    actionPaths.add(actionPath);
//                                }
//                            }
//                        }
//                    }
//                }
//
//                actionPaths = expandOptionalsAndFilterDuplicates(actionPaths);
//
//                for(Path actionPath : actionPaths){
//                    System.out.println(actionPath);
//
//                    Operation epOp = apiOp.copy();
//                    epOp.
//
//
//                    operations.add(epOp);
//
//                }
//
//            }
//        }
//
////        for(int i=0; i<operations.size(); i++) {
////            for (int j = i + 1; j < operations.size(); j++) {
////                Operation op1 = operations.get(i);
////                System.out.println(op1.getPathTemplate());
////            }
////        }
////
////        //-- merge operations with the same path
////        for(int i=0; i<operations.size(); i++){
////            for(int j=i+1; j<operations.size(); j++){
////                Operation op1 = operations.get(i);
////                Operation op2 = operations.get(j);
////                String t1 = op1.getPathTemplate().toString();
////                String t2 = op2.getPathTemplate().toString();
////                if(t1.equalsIgnoreCase(t2)){
////                    op1.actionPathMatches.addAll(op2.actionPathMatches);
////                    operations.remove(j);
////                    j--;
////                }
////            }
////        }
//
//        return operations;
//    }

    List<Path> expandOptionalsAndFilterDuplicates(List<Path> paths){
        List<Path> allPaths = new ArrayList();
        for (Path path : paths)
            allPaths.addAll(path.getSubPaths());

        for (int i = 0; i < allPaths.size(); i++) {
            for (int j = i + 1; j < allPaths.size(); j++) {
                Path p1 = allPaths.get(i);
                Path p2 = allPaths.get(j);

                if (p1.size() != p2.size())
                    continue;

                boolean same = true;

                for (int k = 0; same && k < p1.size(); k++) {
                    if (p1.isVar(k) == p2.isVar(k))
                        continue;
                    else if (p1.isWildcard(k) == p2.isWildcard(k))
                        continue;
                    else {
                        String part1 = p1.get(k);
                        String part2 = p2.get(k);
                        if (!part1.equalsIgnoreCase(part2)) {
                            same = false;
                        }
                    }
                }
                if (same) {
                    allPaths.remove(j);
                    j -= 1;
                }
            }
        }

        return allPaths;
    }




//    List<Path> getOperationPaths(Api api, String method) {
//        List<Path> mergedPaths = new ArrayList();
//        for (Rule.RuleMatcher epMatcher : getIncludeMatchers()) {
//            if(epMatcher.hasMethod(method)){
//                boolean added = false;
//                for(Path epPath : epMatcher.getPaths()){
//                    for(Action action : actions){
//                        Path epPathCopy = new Path(epPath);
//                        List<Path> actionPaths = action.getOperationPaths(api, this, method, epPathCopy);
//                        if(mergedPaths != null && mergedPaths.size() > 0){
//                            mergePaths(mergedPaths, actionPaths);
//                        }
//                    }
//                }
//            }
//        }
//        return mergedPaths;
//    }
//
//
//    List<Path> getOperationPaths(String method, Api api, Path apiPath) {
//
//        List<Path> allPaths = new ArrayList<>();
//
//        List<Action> allActions = new ArrayList<>(this.actions);
//        for (Action action : api.getActions()) {
//            if (!allActions.contains(action))
//                allActions.add(action);
//        }
//        Collections.sort(allActions);
//
//
//        for (Action action : allActions) {
//            for (Path apiSubPath : apiPath.getSubPaths()) {
//                for (int i = 0; i < apiSubPath.size(); i++) {
//                    if (apiPath.isOptional(i)) {
//                        apiSubPath.setOptional(i, true);
//                    }
//
//                    List<Path> actionPaths = action.getOperationPaths(method, api, apiSubPath);
//                    for (Path actionPath : actionPaths) {
//                        Path fullPath = new Path(apiSubPath);
//                        if (fullPath.endsWithWildcard())
//                            fullPath.removeTrailingWildcard();
//                        fullPath.add(actionPath.toString());
//                        allPaths.add(fullPath);
//                    }
//
//                }
//            }
//        }
//
//        return allPaths;
//
//
////        Map<Action, ArrayListValuedHashMap<String, Path>> actionOpPaths = new HashMap();
////
////        for (Action action : actions) {
////            ArrayListValuedHashMap<String, Path> paths = action.getOperationPaths(api);
////            actionOpPaths.put(action, paths);
////        }
////
////
////        for (Rule.RuleMatcher epMatcher : getIncludeMatchers()) {
////            //for (String method : epMatcher.getMethods())
////            {
////                List<Path> epPaths = epMatcher.getPaths();
////                for (Path epPath : epPaths) {
////
////                    Path epOptionalSuffix = epPath.getOptionalSuffix();
////
////                    for(Action action : actions){
////                        List<Path> actionPaths = action.getOperationPaths(method, api);
////                        System.out.println(actionPaths);
////                    }
////
////
////                    System.out.println(epPath + " - " + epOptionalSuffix);
////
////                    List<Path> mergedPaths = new ArrayList();
////                    boolean    hasMatches  = false;
////                    for (Action action : actions) {
////                        if (action.matches(method, epOptionalSuffix)) {
////                            List<Path> actionPaths = actionOpPaths.get(action).get(method);
////                            mergePaths(mergedPaths, actionPaths);
////                            hasMatches = true;
////                        }
////                    }
////
////                    if (hasMatches) {
////                        Path opPath = new Path(epPath.getRequiredPrefix());
////                        if (opPath.size() > 0 && opPath.endsWithWildcard())
////                            opPath.removeTrailingWildcard();
////
////                        if (mergedPaths.size() > 0) {
////                            for (Path actionPath : mergedPaths) {
////                                opPaths.put(method, new Path(opPath).add(actionPath.toString()));
////                            }
////                        } else {
////
////                            List<Path> subPaths = epPath.getSubPaths();
////                            for(int i=0; i<subPaths.size(); i++){
////                                Path sp = subPaths.get(i);
////                                if(i == subPaths.size()-1){
////                                    if(epPath.endsWithWildcard() && !sp.endsWithWildcard())
////                                        sp.add("*");
////                                }
////                                opPaths.put(method, sp);
////                            }
////                        }
////                    }
////                }
////            }
////        }
////
////        return opPaths;
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

    public static boolean compatible(Path a, Path b) {
        for(int i=0; i<a.size(); i++){
            if(a.isWildcard(i))
                return true;
            if(i<b.size() && b.isWildcard(i))
                return true;

            if(i>= b.size())
                return false;

            if(!a.isVar(i) && !b.isVar(i)){
                String sA = a.get(i);
                String sB = b.get(i);
                sA = Path.unwrapOptional(sA);
                sB = Path.unwrapOptional(sB);
                if(!sA.equalsIgnoreCase(sB))
                    return false;
            }
        }
        return true;
    }


    public static List<Path> mergePaths(List<Path> mPaths, List<Path> aPaths) {

        for (Path aPath : aPaths) {

            boolean exists = false;
            for (int m = 0; m < mPaths.size(); m++) {

                Path mPath = mPaths.get(m);
                if (aPath == mPath)
                    continue;

                if(!compatible(aPath, mPath))
                    continue;

                for (int i = 0; i < aPath.size(); i++) {

                    if (i >= mPath.size()) {
                        break;
                    }

                    //the old rule consumes the new one  a/b consumes a/*
                    if (aPath.isWildcard(i) && !mPath.isWildcard(i)) {
                        exists = true;
                        break;
                    }

                    //the new rule will consume the old one....a/* consumed by a/b or a/b/c
                    if (mPath.isWildcard(i)) {
                        mPaths.remove(m);
                        m--;
                        break;
                    }

                    if (!mPath.isVar(i) && aPath.isVar(i))
                        aPath.set(i, mPath.get(i));

                    if (mPath.isVar(i) && !aPath.isVar(i)) {
                        mPath.set(i, aPath.get(i));
                    }

//                    if (!mPath.isVar(i) && !aPath.isVar(i)) {
//                        if (!mPath.get(i).equalsIgnoreCase(aPath.get(i)))
//                            throw ApiException.new500InternalServerError("Endpoint configuration error.  Actions have incompatible paths.");
//                    }


                    if (aPath.size() == mPath.size() && i == mPath.size() - 1)
                        exists = true;
                }
            }

            if (!exists) {
                mPaths.add(aPath);
            }
        }

        //-- removes "a" if "a/*" is in the list.
        for (int i = 0; i < mPaths.size(); i++) {
            for (int j = 0; j < mPaths.size(); j++) {
                if (i == j)
                    continue;

                Path p1 = mPaths.get(i);
                Path p2 = mPaths.get(j);

                if (p1.size() == p2.size() + 1 && p1.endsWithWildcard()) {
                    mPaths.remove(j);
                    j--;
                    continue;
                }
                if (p2.size() == p1.size() + 1 && p2.endsWithWildcard()) {
                    mPaths.remove(i);
                    i--;
                    break;
                }
            }
        }

//        Collections.sort(mPaths, new Comparator<Path>() {
//            @Override
//            public int compare(Path o1, Path o2) {
//                if (o1.size() < o2.size())
//                    return -1;
//                if (o1.size() > o2.size())
//                    return 1;
//                return o1.toString().compareTo(o2.toString());
//            }
//        });

        return mPaths;
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
