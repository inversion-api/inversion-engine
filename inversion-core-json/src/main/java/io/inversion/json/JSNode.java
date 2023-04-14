package io.inversion.json;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.*;

@JsonDeserialize(using = JSNodeDeserializer.class)
public interface JSNode extends JSGet, JSFind {

    Object get(Object key);

    Object put(Object key, Object value);

    Object removeProp(Object key);

    Collection<Object> values();

    Set<String> keySet();

    int size();

    boolean isEmpty();

    void clear();

    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //-- START Methods implemented off of the above abstract methods


    /**
     * @param key
     * @param additionalKeys
     * @return the first non null value from key or addionalKeys
     */
    default Object get(Object key, Object... additionalKeys) {
        Object result = get(key);
        if (result != null)
            return result;
        if (additionalKeys != null && additionalKeys.length > 0) {
            for (int i = 0; i < additionalKeys.length; i++) {
                result = get(additionalKeys[i]);
                if (result != null)
                    return result;
            }
        }
        return null;
    }


    default void putAll(Map map) {
        for (Object key : map.keySet()) {
            put(key, map.get(key));
        }
    }

    default void put(Object key, Object value, Object... nvPairs) {

        put(key, value);

        if (nvPairs == null || nvPairs.length == 0)
            return;

        if (nvPairs.length % 2 != 0)
            throw new RuntimeException("You must supply an even number of arguments to JSNode.with()");

        for (int i = 0; i < nvPairs.length - 1; i += 2) {
            Object val = nvPairs[i + 1];
            put(nvPairs[i].toString(), val);
        }
    }


    default Object remove(Object... keys) {
        Object first = null;
        for (int i = 0; keys != null && i < keys.length; i++) {
            Object key = keys[i];
            if (key != null) {
                Object removed = removeProp(key);
                if (first == null && removed != null)
                    first = removed;
            }
        }
        return first;
    }


    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //-- Utility Methods

    default JSNode getJson() {
        return this;
    }

    default boolean isList() {
        return this instanceof List;
    }

    default boolean isMap() {
        return this instanceof Map;
    }

    default <T> T as(Class<T> type){
        return JSParser.parseJson(this, type);
    }

    default JSList asList() {
        if (this instanceof JSList)
            return (JSList) this;

        JSList list = new JSList();
        list.add(this);
        return list;
    }

    default List<JSMap> asMapList() {
        return (List<JSMap>) asList();
    }

    default List<JSNode> asNodeList() {
        return (List<JSNode>) asList();
    }


    default JSNode copy() {
        return JSParser.asJSNode(toString());
    }


    /**
     * Prints the JSNode with properties written out in their original case.
     *
     * @param pretty should spaces and carriage returns be added to the doc for readability
     * @return json string, with properties written out in their original case optional pretty printed.
     */
    default String toString(boolean pretty) {
        return JSWriter.toJson(this, pretty, false);
    }

    /**
     * Prints the JSNode
     *
     * @param pretty                 should spaces and carriage returns be added to the doc for readability
     * @param lowercasePropertyNames when true all property names are printed in lower case instead of their original case
     * @return a json string
     */
    default String toString(boolean pretty, boolean lowercasePropertyNames) {
        return JSWriter.toJson(this, pretty, lowercasePropertyNames);
    }

    default void visit(JSVisitor visitor) {
        visit0(visitor, new IdentityHashMap<>(), new JSPointer(null, null, this));
    }

    default boolean visit0(JSVisitor visitor, IdentityHashMap<JSNode, JSNode> visited, JSPointer currentPath) {
        JSNode node = currentPath.getNode();
        if (!visitor.visit(currentPath))
            return false;

        if (visited.containsKey(node))
            return true;

        visited.put(node, node);

        for (String key : keySet()) {
            Object value = get(key);
            if (value instanceof JSNode) {
                JSPointer child = new JSPointer(currentPath, key, (JSNode) value);
                if (!((JSNode) value).visit0(visitor, visited, child))
                    return false;
            }
        }
        return true;
    }


    /**
     * @return an iterator created off of calling visitIterator();
     * @see #visitIterator()
     */
    default Iterable<JSPointer> visit() {
        return () -> visitIterator();
    }

    /**
     * This is the best way to iterate through all nodes in a document.  The first JSPointer returned
     * is to this node then its childern in depth first traversal order recursively.  If a circular
     * reference exists, the repeated node will be returned but its children will not be traversed
     * a second time.
     *
     * @return iterator that returns this node then all child nodes recursively.
     * @see #visit()
     */
    default Iterator<JSPointer> visitIterator() {
        final Map<JSPointer, Iterator> keysMap = new IdentityHashMap<>();
        final Map<JSNode, JSNode>      visited = new IdentityHashMap();

        Iterator it = new Iterator() {

            JSPointer last = null;
            JSPointer next = new JSPointer(null, null, JSNode.this);

            @Override
            public boolean hasNext() {
                if (next != null)
                    return true;
                next = findNext(last);
                return next != null;
            }

            @Override
            public Object next() {
                if (next == null)
                    next = findNext(last);
                last = next;
                next = null;
                return last;
            }

            JSPointer findNext(JSPointer parent) {
                JSPointer next = null;
                while (next == null && parent != null) {
                    Iterator keys = keysMap.get(parent);
                    if (keys == null && !visited.containsKey(parent.getNode())) {
                        keys = parent.node.keySet().iterator();
                        keysMap.put(parent, keys);
                        visited.put(parent.getNode(), parent.getNode());
                    }

                    if (keys != null) {
                        while (keys.hasNext()) {
                            Object key   = keys.next();
                            Object child = parent.node.get(key);
                            if (child instanceof JSNode) {
                                next = new JSPointer(parent, key, (JSNode) child);
                                break;
                            }
                        }
                    }
                    if (next == null) {
                        parent = parent.getParent();
                    }
                }
                return next;
            }
        };
        return it;
    }

}

