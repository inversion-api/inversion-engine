package io.inversion.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inversion.utils.Utils;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class JSNode implements JSGet, JSFind, JSDiff, JSPatch {

    protected static ObjectMapper mapper = new ObjectMapper();

    protected abstract JSProperty getProperty(Object key);

    protected abstract List<JSProperty> getProperties();

    protected abstract JSProperty putProperty(JSProperty property);

    protected abstract boolean removeProperty(JSProperty property);

    public abstract void clear();

    public final int size() {
        return getProperties().size();
    }


    public final boolean isEmpty() {
        return getProperties().size() == 0;
    }

    public Object getValue(Object key) {
        JSProperty property = getProperty(key);
        if (property != null)
            return property.getValue();
        return null;
    }

    public List getValues() {
        return getProperties().stream().map(p -> p.getValue()).collect(Collectors.toList());
    }

    public Object putValue(Object key, Object value) {
        return putProperty(new JSProperty(key, value));
    }

    public Object removeValues(Object... keys) {
        Object first = null;
        for (int i = 0; keys != null && i < keys.length; i++) {
            Object key = keys[i];
            if (key != null) {
                JSProperty prop = getProperty(key);
                if (prop != null) {
                    removeProperty(prop);
                    if (first == null)
                        first = prop.getValue();
                }
            }
        }
        return first;
    }

    public Set<String> keySet() {
        LinkedHashSet keySet = new LinkedHashSet();
        getProperties().forEach(p -> keySet.add(p.getKey().toString()));
        return keySet;
    }

    public final boolean isList() {
        return this instanceof JSList;
    }

    public final JSList asList() {
        if (this instanceof JSList)
            return (JSList) this;

        JSList list = new JSList();
        list.add(this);
        return list;
    }

    public final List<JSMap> asMapList() {
        return (List<JSMap>) asList();
    }


    public final List<JSNode> asNodeList() {
        return (List<JSNode>) asList();
    }



    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //-- JSGet, JSFind, JSDiff, JSPatch Interface Methods


    public JSNode getJson() {
        return this;
    }


    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //-- Utility Methods

    /**
     * Convenience method that calls asList().stream().
     *
     * @return asList().stream()
     */
    public Stream stream() {
        return asList().stream();
    }

    public void with(Object... nvPairs) {
        if (nvPairs == null || nvPairs.length == 0)
            return;

        if (nvPairs.length % 2 != 0)
            throw Utils.ex("You must supply an even number of arguments to JSNode.with()");

        for (int i = 0; i < nvPairs.length - 1; i += 2) {
            Object value = nvPairs[i + 1];
            putValue(nvPairs[i], value);
        }
    }


    public JSNode copy() {
        return JSParser.asJSNode(toString());
    }

    /**
     * @return json string, pretty printed with properties written out in their original case.
     */
    @Override
    public String toString() {
        return JSWriter.toJson(this, true, false);
    }


    /**
     * Prints the JSNode with properties written out in their original case.
     *
     * @param pretty should spaces and carriage returns be added to the doc for readability
     * @return json string, with properties written out in their original case optional pretty printed.
     */
    public String toString(boolean pretty) {
        return JSWriter.toJson(this, pretty, false);
    }

    /**
     * Prints the JSNode
     *
     * @param pretty                 should spaces and carriage returns be added to the doc for readability
     * @param lowercasePropertyNames when true all property names are printed in lower case instead of their original case
     * @return a json string
     */
    public String toString(boolean pretty, boolean lowercasePropertyNames) {
        return JSWriter.toJson(this, pretty, lowercasePropertyNames);
    }


    public void visit(JSVisitor visitor) {
        visit(visitor, this, new Stack<>(), new IdentityHashMap<>());
    }

    void visit(JSVisitor visitor, JSNode node, Stack<Triple<JSNode, JSProperty, Object>> path, IdentityHashMap<JSNode, JSNode> visited) {
        if (visited.containsKey(node))
            return;

        visited.put(node, node);

        if (node instanceof JSNode) {
            if (visitor.visit(node, path)) {
                List<JSProperty> props = node.getProperties();
                for (int i = 0; i < props.size(); i++) {
                    JSProperty prop  = props.get(i);
                    Object     value = prop.getValue();
                    if (value instanceof JSNode) {
                        Triple triple = Triple.of(node, prop, value);
                        path.push(triple);
                        visit(visitor, (JSNode) value, path, visited);
                        path.pop();
                    }
                }
            }
        }
    }
//
//
//
//    public void visitNodes(JSNodeVisitor visitor) {
//        visit(visitor, this, new Stack<>(), new IdentityHashMap<>());
//    }
//
//    void visit(JSNodeVisitor visitor, JSNode node, Stack<Triple<JSNode, JSProperty, Object>> path, IdentityHashMap<JSNode, JSNode> visited) {
//
//        if (visited.containsKey(node))
//            return;
//
//        visited.put(node, node);
//
//        if (node instanceof JSNode) {
//            if (!visitor.visit(node, path)) {
//                return;
//            }
//
//            List<JSProperty> props = node.getProperties();
//            for (int i = 0; i < props.size(); i++) {
//                JSProperty prop  = props.get(i);
//                Object     value = prop.getValue();
//
//                Triple triple = Triple.of(node, prop, value);
//                path.push(triple);
//                try {
//                    if (value instanceof JSNode) {
//                        visit(visitor, (JSNode) value, path, visited);
//                    }
//                } finally {
//                    path.pop();
//                }
//            }
//        }
//    }
//
//
//    public void visitProperties(JSPropertyVisitor visitor) {
//        visit(visitor, this, new Stack<>(), new IdentityHashMap<>());
//    }
//
//    void visit(JSPropertyVisitor visitor, JSNode node, Stack<Triple<JSNode, JSProperty, Object>> path, IdentityHashMap<JSNode, JSNode> visited) {
//
//        if (visited.containsKey(node))
//            return;
//
//        visited.put(node, node);
//
//        List<JSProperty> props = node.getProperties();
//        for (int i = 0; i < props.size(); i++) {
//            JSProperty prop  = props.get(i);
//            Object     value = prop.getValue();
//
//            Triple triple = Triple.of(node, prop, value);
//            path.push(triple);
//            try {
//                if (visitor.visit(node, prop, path)) {
//                    if (value instanceof JSNode) {
//                        visit(visitor, (JSNode) value, path, visited);
//                    }
//                }
//            } finally {
//                path.pop();
//            }
//        }
//    }


}

