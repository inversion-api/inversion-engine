package io.inversion.json;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class JSNode implements JSGet, JSFind, JSDiff, JSPatch {

    protected abstract JSProperty getProperty(Object key);

    protected abstract List<JSProperty> getProperties();

    protected abstract JSProperty putProperty(JSProperty property);

    protected abstract boolean removeProperty(JSProperty property);

    public abstract void clear();

    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //-- START Methods implemented off of the above abstract methods

    public final int size() {
        return getProperties().size();
    }

    public final boolean isEmpty() {
        return getProperties().size() == 0;
    }

    public Object get(Object key) {
        JSProperty property = getProperty(key);
        if (property != null)
            return property.getValue();
        return null;
    }

    public Object get(String key) {
        return get((Object) key);
    }

    public Object get(int key) {
        return get((Object) key);
    }

    public Object put(Object key, Object value) {
        return putProperty(new JSProperty(key, value));
    }

    public Object put(String key, Object value) {
        return putProperty(new JSProperty(key, value));
    }

    public Object put(int key, Object value) {
        return putProperty(new JSProperty(key, value));
    }

    public void putAll(Map map){
        for(Object key : map.keySet()){
            put(key, map.get(key));
        }
    }

    public void putAll(Object... nvPairs) {

        if (nvPairs == null || nvPairs.length == 0)
            return;

        if(nvPairs.length == 1 && nvPairs[0] instanceof Map){
            putAll((Map)nvPairs[0]);
            return;
        }

        if (nvPairs.length % 2 != 0)
            throw new RuntimeException("You must supply an even number of arguments to JSNode.with()");

        for (int i = 0; i < nvPairs.length - 1; i += 2) {
            Object val = nvPairs[i + 1];
            put(nvPairs[i].toString(), val);
        }
    }

//    public Object remove(Object key) {
//        JSProperty prop = getProperty(key);
//        if (prop != null) {
//            removeProperty(prop);
//            return prop.value;
//        }
//        return null;
//    }

    public Object remove(Object... keys) {
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

    public Collection values() {
        return getProperties().stream().map(p -> p.getValue()).collect(Collectors.toList());
    }

    public Set keySet() {
        LinkedHashSet keySet = new LinkedHashSet();
        getProperties().forEach(p -> keySet.add(p.getKey()));
        return keySet;
    }

    public boolean containsKey(Object key) {
        if (key == null)
            return false;
        return getProperty(key.toString()) != null;
    }

    public boolean containsValue(Object value) {
        for (JSProperty p : getProperties()) {
            if (value == null && p.getValue() == null
                    || value != null && value.equals(p.getValue()))
                return true;
        }
        return false;
    }

    /**
     * @return all property name / value pairs
     */
    public Set<Map.Entry<String, Object>> entrySet() {
        Map map = new LinkedHashMap();
        getProperties().forEach(p -> map.put(p.getKey(), p.getValue()));
        return map.entrySet();
    }

    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //-- Utility Methods

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


    public JSNode getJson() {
        return this;
    }

    public final List<JSMap> asMapList() {
        return (List<JSMap>) asList();
    }


    public final List<JSNode> asNodeList() {
        return (List<JSNode>) asList();
    }


    /**
     * Convenience method that calls asList().stream().
     *
     * @return asList().stream()
     */
    public Stream stream() {
        return asList().stream();
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
        visit0(visitor, new IdentityHashMap<>(), new JSPath(null, null, this));
    }

    boolean visit0(JSVisitor visitor, IdentityHashMap<JSNode, JSNode> visited, JSPath path) {

        JSNode node = path.getNode();
        if (visited.containsKey(node))
            return true;

        visited.put(node, node);

        if (!visitor.visit(path))
            return false;

        List<JSProperty> props = node.getProperties();
        for (int i = 0; i < props.size(); i++) {
            JSProperty prop  = props.get(i);
            Object     value = prop.getValue();
            if (value instanceof JSNode) {
                JSPath child = new JSPath(path, prop.getKey(), (JSNode) value);
                if (!((JSNode) value).visit0(visitor, visited, path))
                    return false;
            }
        }
        return true;
    }

}

