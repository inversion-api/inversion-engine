package io.inversion.json;

import java.util.*;

public class JSMap <T> extends JSNode implements Map<String, T> {

    /**
     * Maps the lower case JSProperty.name to the property for case
     * insensitive lookup with the ability to preserve the original
     * case.
     */
    LinkedHashMap<String, JSProperty> properties = new LinkedHashMap<>();

    /**
     * Creates an empty JSNode.
     */
    public JSMap() {

    }

    /**
     * Creates a JSNode with <code>nameValuePairs</code> as the initial properties.
     * <p>
     * The first and every other element in <code>nameValuePairs</code> should be a string.
     *
     * @param nameValuePairs the name value pairs to add
     * @see #with(Object...)
     */
    public JSMap(Object... nameValuePairs) {
        with(nameValuePairs);
    }

    /**
     * Creates a JSNode with <code>nameValuePairs</code> as the initial properties.
     *
     * @param nameValuePairs the name value pairs to add
     * @see #putAll(Map)
     */
    public JSMap(Map nameValuePairs) {
        putAll(nameValuePairs);
    }

    @Override
    protected JSProperty getProperty(Object key) {
        if (key == null)
            return null;
        return this.properties.get(key.toString().toLowerCase());
    }

    @Override
    protected List<JSProperty> getProperties() {
        return new ArrayList(properties.values());
    }

    @Override
    protected JSProperty putProperty(JSProperty property) {
        JSProperty old = this.properties.put(property.getKey().toString().toLowerCase(), property);
        return old;
    }

    @Override
    protected boolean removeProperty(JSProperty property) {
        JSProperty old = properties.remove(property.getKey().toString().toLowerCase());
        return old != null;
    }

    @Override
    public void clear() {
        properties.clear();
    }


    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //-- Map Implementations

    @Override
    public T get(Object key) {
        return (T)this.getValue(key);
    }

    @Override
    public Object put(String key, Object value) {
        return putValue(key, value);
    }

    public Object putFirst(String key, Object value){
        JSProperty oldProp = getProperty(key);
        properties.remove(key);

        LinkedHashMap<String, JSProperty> oldMap = properties;
        properties = new LinkedHashMap<>();

        put(key, value);
        for(JSProperty prop : oldMap.values()){
            putProperty(prop);
        }
        return oldProp != null ? oldProp.getValue() : null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends T> map) {
        for (String key : map.keySet()) {
            put(key, map.get(key));
        }
    }

    @Override
    public T remove(Object key) {
        JSProperty prop = getProperty(key);
        if (key != null)
            removeProperty(prop);
        return prop != null ? (T)prop.getValue() : null;
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null)
            return false;
        return getProperty(key.toString()) != null;
    }

    @Override
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
    @Override
    public Set<Map.Entry<String, T>> entrySet() {
        Map map = new LinkedHashMap();
        getProperties().forEach(p -> map.put(p.getKey(), p.getValue()));
        return map.entrySet();
    }

    @Override
    public Collection<T> values() {
        return getValues();
    }

    /**
     * Changes the property name iteration order from insertion order to alphabetic order.
     */
    public void sortKeys() {
        List<String> keys = new ArrayList(keySet());
        Collections.sort(keys);
        LinkedHashMap<String, JSProperty> newProps = new LinkedHashMap();
        for (String key : keys) {
            newProps.put(key, getProperty(key));
        }
        clear();
        newProps.values().forEach(p -> putValue(p.getKey(), p.getValue()));
    }
}
