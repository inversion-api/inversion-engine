package io.inversion.json;

import io.inversion.utils.Utils;

import java.util.*;
import java.util.stream.Stream;

public interface JSFind {

    JSNode getJson();

    /**
     * A heroically permissive finder supporting JSON Pointer, JSONPath and
     * a simple 'dot and wildcard' type of system like so:
     * 'propName.childPropName.*.skippedGenerationPropsName.4.fifthArrayNodeChildPropsName.**.recursivelyFoundPropsName'.
     *
     * <p>
     * All forms are internally converted into a 'master' form before processing.  This master
     * simply uses '.' to separate property names and array indexes and uses uses '*' to represent
     * a single level wildcard and '**' to represent a recursive wildcard.  For example:
     * <ul>
     *   <li>'myProp' finds 'myProp' in this node.
     *   <li>'myProp.childProp' finds 'childProp' on 'myProp'
     *   <li>'myArrayProp.2.*' finds all properties of the third element of the 'myArrayProp'
     *   <li>'*.myProp' finds 'myProp' in any of the children of this node.
     *   <li>'**.myProp' finds 'myProp' anywhere in my descendents.
     *   <li>'**.myProp.*.value' finds 'value' as a grandchild anywhere under me.
     *   <li>'**.*' returns every element of the document.
     *   <li>'**.5' gets the 6th element of every array.
     *   <li>'**.book[?(&#064;.isbn)]' finds all books with an isbn
     *   <li>'**.[?(&#064;.author = 'Herman Melville')]' finds all book with author 'Herman Melville'
     * </ul>
     * <p>
     * Arrays indexes are treated just like property names but with integer names.
     * For example "myObject.4.nextProperty" finds "nextProperty" on the 5th element
     * in the "myObject" array.
     *
     * <p>
     * JSON Pointer is the least expressive supported form and uses '/' characters to separate properties.
     * To support JSON Pointer, we simply replace all '/' characters for "." characters before
     * processing.
     *
     * <p>
     * JSON Path is more like XML XPath but uses '.' instead of '/' to separate properties.
     * Technically JSON Path statements are supposed to start with '$.' but that is optional here.
     * The best part about JSON Path is the query filters that let you conditionally select
     * elements.
     * <p>
     * Below is the implementation status of various JSON Path features:
     * <ul>
     *  <li>SUPPORTED $.store.book[*].author                     //the authors of all books in the store
     *  <li>SUPPORTED $..author                                  //all authors
     *  <li>SUPPORTED $.store..price                             //the prices of all books
     *  <li>SUPPORTED $..book[2]                                 //the third book
     *  <li>SUPPORTED $..book[?(@.price@lt;10)]                  //all books priced @lt; 10
     *  <li>SUPPORTED $..[?(@.price@lt;10)]                      //find any node with a price property
     *  <li>SUPPORTED $..[?(@.*.price@lt;10)]                    //find the parent of any node with a price property
     *  <li>SUPPORTED $..book[?(@.author = 'Herman Melville')]   //all books where 'Herman Melville' is the author
     *  <li>SUPPORTED $..*                                       //all members of JSON structure.
     *  <li>SUPPORTED      $..book[(@.length-1)]                 //the last book in order
     *  <li>SUPPORTED      $..book[-1:]                          //the last book in order
     *  <li>SUPPORTED      $..book[0,1]                          //the first two books
     *  <li>SUPPORTED      $..book[:2]                           //the first two books
     *  <li>SUPPORTED $..book[?(@.isbn)]                         //find all books with an isbn property
     *  <li>SUPPORTED $..[?(@.isbn)]                             //find any node with an isbn property
     *  <li>SUPPORTED $..[?(@.*.isbn)]                           //find the parent of any node with an isbn property
     *  <li>SUPPORTED $..[?(@.*.*.isbn)]                         //find the grandparent of any node with an isbn property
     *
     *
     * </ul>
     * <p>
     * The JSON Path following boolean comparison operators are supported:
     * <ul>
     *  <li> =
     *  <li>@gt;
     *  <li>@lt;
     *  <li>@gt;=
     *  <li>@lt;=
     *  <li>!=
     * </ul>
     *
     * <p>
     * JsonPath bracket-notation such as  "$['store']['book'][0]['title']"
     * is currently not supported.
     *
     * @param pathExpression defines the properties to find
     * @param qty            the maximum number of results
     * @return an array of found values
     * @see <a href="https://tools.ietf.org/html/rfc6901">JSON Pointer</a>
     * @see <a href="https://goessner.net/articles/JsonPath/">JSON Path</a>
     * @see <a href="https://github.com/json-path/JsonPath">JSON Path</a>
     */
    default public JSList findAll(String pathExpression, int qty) {
        pathExpression = fromJsonPointer(pathExpression);
        pathExpression = fromJsonPath(pathExpression);
        return new JSList(findAll0(pathExpression, qty, new ArrayList(), new HashMap()));
    }

    default List findAll0(String pathExpression, int qty, List collected, HashMap<String, Set<JSNode>> visited) {
        JSONPathTokenizer tok = new JSONPathTokenizer(//
                "['\"", //openQuoteStr
                "]'\"", //closeQuoteStr
                "]", //breakIncludedChars
                ".", //breakExcludedChars
                "", //unquotedIgnoredChars
                ". \t", //leadingIgnoredChars
                pathExpression //chars
        );

        List<String> path = tok.asList();
        return findAll0(path, qty, collected, visited);
    }


    default List findAll0(List<String> path, int qty, List collected, HashMap<String, Set<JSNode>> visited) {

        JSNode json = getJson();

        //-- infinite recursion protection
        //-- you can visit a path more than once trying different parts of the search path
        //-- but you can only visit a node once for any given permutation of the path.
        String pathStr = path.toString();
        Set    old     = visited.get(pathStr);
        if (old == null) {
            old = new HashSet();
            visited.put(pathStr, old);
        }
        if (old.contains(this))
            return collected;
        old.add(this);
        //-- end infinite recursion protection


        if (qty > 1 && collected.size() >= qty)
            return collected;

        String nextSegment = path.get(0);

        if ("*".equals(nextSegment)) {
            if (path.size() == 1) {
                Collection values = json.getValues();

                for (Object value : values) {
                    if (!collected.contains(value) && (qty < 1 || collected.size() < qty))
                        collected.add(value);
                }
            } else {
                List<String> nextPath = path.subList(1, path.size());
                for (Object value : json.getValues()) {
                    if (value instanceof JSNode) {
                        ((JSNode) value).findAll0(nextPath, qty, collected, visited);
                    }
                }
            }
        } else if ("**".equals(nextSegment)) {
            if (path.size() != 1) {
                List<String> nextPath = path.subList(1, path.size());
                this.findAll0(nextPath, qty, collected, visited);
                for (Object value : json.getValues()) {
                    if (value instanceof JSNode) {
                        ((JSNode) value).findAll0(path, qty, collected, visited);
                    }
                }
            }
        }
        //else if (this instanceof JSList && nextSegment.startsWith("[") && nextSegment.endsWith("]")){
        else if (nextSegment.startsWith("[") && nextSegment.endsWith("]")) {
            //this is a JSONPath filter that is not just an array index
            String expr = nextSegment.substring(1, nextSegment.length() - 1).trim();
            if (expr.startsWith("?(") && expr.endsWith(")")) {
                JSONPathTokenizer tokenizer = new JSONPathTokenizer(//
                        "'\"", //openQuoteStr
                        "'\"", //closeQuoteStr
                        "?=<>!", //breakIncludedChars...breakAfter
                        "]=<>! ", //breakExcludedChars...breakBefore
                        "[()", //unquotedIgnoredChars
                        "]. \t", //leadingIgnoredChars
                        expr);

                String token;
                String func    = null;
                String subpath = null;
                String op      = null;
                String value   = null;

                //-- Choices after tokenization
                //-- $..book[2]  -> 2
                //-- $..book[author] -> author
                //-- $..book[(@.length-1)] -> @_length-1
                //-- $..book[-1:] -> -1:
                //-- $..book[0,1] -> 0,1
                //-- $..book[:2] -> :2
                //-- $..book[?(@.isbn)] -> ? @_isbn
                //-- $..book[?(@.price<10)] -> ?


                while ((token = tokenizer.next()) != null) {
                    if (token.equals("?")) {
                        func = "?";
                        continue;
                    }

                    if (token.startsWith("@_")) {
                        subpath = token.substring(2);
                    } else if (Utils.in(token, "=", ">", "<", "!")) {
                        if (op == null)
                            op = token;
                        else
                            op += token;
                    } else if (subpath != null && op != null && value == null) {
                        value = token;

                        if (json.isList()) {
                            for (Object child : json.getValues()) {
                                if (child instanceof JSNode) {
                                    List found = ((JSNode) child).findAll0(subpath, -1, new ArrayList(), visited);
                                    for (Object val : found) {
                                        if (eval(val, op, value)) {
                                            if (!collected.contains(child) && (qty < 1 || collected.size() < qty))
                                                collected.add(child);
                                        }
                                    }
                                }
                            }
                        } else {
                            List found = findAll0(subpath, -1, new ArrayList(), visited);
                            for (Object val : found) {
                                if (eval(val, op, value)) {
                                    if (!collected.contains(this) && (qty < 1 || collected.size() < qty)) {
                                        collected.add(this);
                                        break;
                                    }
                                }
                            }
                        }

                        func = null;
                        subpath = null;
                        op = null;
                        value = null;
                    }
                }
                //$..book[?(@.isbn)] -- checks for the existence of a property
                if ("?".equals(func) && subpath != null) {
                    if (op != null || value != null) {
                        //unparseable...do nothing
                    }

                    if (json.isList()) {
                        for (Object child : json.getValues()) {
                            if (child instanceof JSNode) {
                                List found = ((JSNode) child).findAll0(subpath, -1, new ArrayList(), visited);
                                for (Object val : found) {
                                    if (!collected.contains(child) && (qty < 1 || collected.size() < qty))
                                        collected.add(child);
                                }
                            }
                        }
                    } else {
                        List found = findAll0(subpath, -1, new ArrayList(), visited);
                        if (found.size() > 0) {
                            if (!collected.contains(this) && (qty < 1 || collected.size() < qty))
                                collected.add(this);
                        }
                    }
                }
            } else {
                //-- $..book[(@.length-1)] -> @_length-1
                //-- $..book[-1:] -> -1:
                //-- $..book[0,1] -> 0,1
                //-- $..book[:2] -> :2
                if (json.isList()) {

                    int length = ((JSList) this).size();

                    List found = new ArrayList();
                    if (expr.startsWith("(@_length-")) {
                        int index = Integer.parseInt(expr.substring(expr.indexOf("-") + 1, expr.length() - 1).trim());
                        if (length - index > 0) {
                            found.add(json.getValue(length - index));
                        }
                    } else if (expr.startsWith(":")) {
                        int count = Integer.parseInt(expr.substring(1).trim());
                        for (int i = 0; i < length && i < count; i++) {
                            found.add(json.getValue(count));
                        }
                    } else if (expr.endsWith(":")) {
                        int idx = Integer.parseInt(expr.substring(0, expr.length() - 1).trim()) * -1;
                        if (idx <= length)
                            found.add(json.getValue(length - idx));
                    } else {
                        try {

                            int start = Integer.parseInt(expr.substring(0, expr.indexOf(":")).trim());
                            int end   = Integer.parseInt(expr.substring(expr.indexOf(":") + 1).trim());
                            for (int i = start; i <= end && i < length; i++) {
                                found.add(json.getValue(i));
                            }
                        } catch (Exception ex) {
                            System.out.println(expr);
                            ex.printStackTrace();
                        }
                    }
                    if (found.size() > 0) {
                        if (path.size() > 1) {
                            List<String> nextPath = path.subList(1, path.size());
                        } else {
                            collected.addAll(found);
                        }
                    }
                }
            }

        } else {
            Object found = null;
            try {
                if(json instanceof JSList && Utils.atoi(nextSegment) < 0)
                    System.out.println("asdf");
                found = json.getValue(nextSegment);
            } catch (NumberFormatException ex) {
                //trying to access an array with a prop name...ignore
            }
            if (found != null) {
                if (path.size() == 1) {
                    if (!collected.contains(found) && (qty < 1 || collected.size() < qty))
                        collected.add(found);
                } else if (found instanceof JSNode) {
                    ((JSNode) found).findAll0(path.subList(1, path.size()), qty, collected, visited);
                }
            }
        }

        return collected;
    }


    /**
     * Convenience overloading of {@link #findAll(String, int)} that returns the first item found
     *
     * @param pathExpression specifies the properties to find
     * @return the first item found at <code>pathExpression</code>
     * @see #findAll(String, int)
     */
    default Object find(String pathExpression) {
        JSList found = findAll(pathExpression, 1);
        if (found.size() > 0)
            return found.get(0);

        return null;
    }

    /**
     * Convenience overloading of {@link #find(String)}
     *
     * @param pathExpression specifies the nodes to find
     * @return the first value found at <code>pathExpression</code> cast as a JSMap if exists else null
     * @throws ClassCastException if the object found is not a JSNode
     * @see #find(String)
     */
    default JSMap findMap(String pathExpression) {
        return (JSMap) find(pathExpression);
    }

    /**
     * Convenience overloading of {@link #find(String)}
     *
     * @param pathExpression specifies the nodes to find
     * @return the first value found at <code>pathExpression</code> cast as a JSNode if exists else null
     * @throws ClassCastException if the object found is not a JSNode
     * @see #find(String)
     */
    default JSNode findNode(String pathExpression) {
        return (JSNode) find(pathExpression);
    }

    /**
     * Convenience overloading of {@link #find(String)}
     *
     * @param pathExpression specifies the properties to find
     * @return the first value found at <code>pathExpression</code> cast as a JSList if exists else null
     * @throws ClassCastException if the object found is not a JSList
     * @see #find(String)
     */
    default JSList findList(String pathExpression) {
        return (JSList) find(pathExpression);
    }

    /**
     * Convenience overloading of {@link #find(String)}
     *
     * @param pathExpression specifies the properties to find
     * @return the first value found at <code>pathExpression</code> stringified if exists else null
     * @see #find(String)
     */
    default String findString(String pathExpression) {
        Object found = find(pathExpression);
        if (found != null)
            return found.toString();

        return null;
    }

    /**
     * Convenience overloading of {@link #find(String)}
     *
     * @param pathExpression specifies the properties to find
     * @return the first value found at <code>pathExpression</code> stringified and parsed as an int if exists else -1
     * @see #find(String)
     */
    default int findInt(String pathExpression) {
        Object found = find(pathExpression);
        if (found != null)
            return Utils.atoi(found);

        return -1;
    }


    /**
     * Convenience overloading of {@link #find(String)}
     *
     * @param pathExpression specifies the properties to find
     * @return the first value found at <code>pathExpression</code> stringified and parsed as a long if exists else -1
     * @see #find(String)
     */
    default long findLong(String pathExpression) {
        Object found = find(pathExpression);
        if (found != null)
            return Utils.atol(found);

        return -1;
    }

    /**
     * Convenience overloading of {@link #find(String)}
     *
     * @param pathExpression specifies the properties to find
     * @return the first value found at <code>pathExpression</code> stringified and parsed as a double if exists else -1
     * @see #find(String)
     */
    default double findDouble(String pathExpression) {
        Object found = find(pathExpression);
        if (found != null)
            return Utils.atod(found);

        return -1;
    }

    /**
     * Convenience overloading of {@link #find(String)}
     *
     * @param pathExpression specifies the properties to find
     * @return the first value found at <code>pathExpression</code> stringified and parsed as a boolean if exists else false
     * @see #find(String)
     */
    default boolean findBoolean(String pathExpression) {
        Object found = find(pathExpression);
        if (found != null)
            return Utils.atob(found);

        return false;
    }

    /**
     * Convenience overloading of {@link #findAll(String, int)}
     *
     * @param pathExpression specifies the properties to find
     * @return all items found for <code>pathExpression</code>
     * @see #findAll(String, int)
     */
    default JSList findAll(String pathExpression) {
        return findAll(pathExpression, -1);
    }

    /**
     * Convenience overloading of {@link #findAll(String, int)}
     *
     * @param pathExpression specifies the properties to find
     * @return all items found for <code>pathExpression</code> cast as a List
     * @see #findAll(String, int)
     */
    default List<JSNode> findAllNodes(String pathExpression) {
        return (List<JSNode>)findAll(pathExpression);
    }

    default Stream streamAll() {
        List all = findAll("**.*").asList();
        all.add(this);
        return all.stream();
    }

    static boolean eval(Object var, String op, Object value) {
        value = Utils.dequote(value.toString());

        if (var instanceof Number) {
            try {
                value = Double.parseDouble(value.toString());
            } catch (Exception ex) {
                //ok, value was not a number...ignore
            }
        }

        if (var instanceof Boolean) {
            try {
                value = Boolean.parseBoolean(value.toString());
            } catch (Exception ex) {
                //ok, value was not a boolean...ignore
            }
        }

        int comp = ((Comparable) var).compareTo(value);

        switch (op) {
            case "=":
                return comp == 0;
            case ">":
                return comp > 0;
            case ">=":
                return comp >= 0;
            case "<":
                return comp < 0;
            case "<=":
                return comp <= 0;
            case "!=":
                return comp != 0;
            default:
                throw new UnsupportedOperationException("Unknown operator '" + op + "'");
        }
    }

    /**
     * Simply replaces "/" with "."
     * <p>
     * Slashes in property names (seriously a stupid idea anyway) which is supported
     * by JSON Pointer is not supported.
     *
     * @param jsonPointer a slash based path expression
     * @return a dot based path expression
     */
    static String fromJsonPointer(String jsonPointer) {
        if (jsonPointer.charAt(0) == '#') {
            jsonPointer = jsonPointer.substring(1);
            if (jsonPointer.charAt(0) == '/')
                jsonPointer = jsonPointer.substring(1);
        }


        return jsonPointer.replace('/', '.');
    }

    /**
     * Converts a proper json path statement into its "relaxed dotted wildcard" form
     * so that it is easier to parse.
     */
    static String fromJsonPath(String jsonPath) {
        if (jsonPath.charAt(0) == '#') {
            jsonPath = jsonPath.substring(1);
        }

        if (jsonPath.charAt(0) == '$')
            jsonPath = jsonPath.substring(1);

        jsonPath = jsonPath.replace("@.", "@_"); //from jsonpath spec..switching to "_" to make parsing easier
        jsonPath = jsonPath.replaceAll("([a-zA-Z])\\[", "$1.["); //from json path spec array[index] converted to array.[index]. to support array.index.value legacy format.
        jsonPath = jsonPath.replace("..", "**."); //translate from jsonpath format
        jsonPath = jsonPath.replaceAll("([a-zA-Z])[*]", "$1.*"); //translate from jsonpath format
        jsonPath = jsonPath.replaceAll("([a-zA-Z])\\[([0-9]*)\\]", "$1.$2"); // x[1] to x.1
        jsonPath = jsonPath.replaceAll("([a-zA-Z])\\[([0-9]*)\\]", "$1.$2"); // x[1] to x.1
        jsonPath = jsonPath.replaceAll("\\.\\[([0-9]*)\\]", ".$1"); //translate .[1]. to .1. */
        jsonPath = jsonPath.replaceAll("\\[([0-9]*)\\]", "$1"); // [123] to 123 ...catches a root array
        jsonPath = jsonPath.replace("[*]", "*");

        //System.out.println(pathStr);
        return jsonPath;
    }
}
