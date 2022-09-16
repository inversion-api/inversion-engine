package io.inversion.json;

import java.util.*;

public class JSMap extends JSNode implements Map {

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
     * @see #putAll(Object...)
     */
    public JSMap(Object... nameValuePairs) {
        putAll(nameValuePairs);
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
    //-- START Additional Map Interface Specific Methods


    @Override
    public Object remove(Object key) {
        JSProperty prop = getProperty(key);
        if (prop != null)
            removeProperty(prop);
        return prop != null ? prop.getValue() : null;
    }



    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //-- END Additional Map Interface Methods



    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //-- Utility Methods



    /**
     * Changes the property name iteration order from insertion order to alphabetic order.
     */
    public void sort() {
        List<String> keys = new ArrayList(keySet());
        Collections.sort(keys);
        LinkedHashMap<String, JSProperty> newProps = new LinkedHashMap();
        for (String key : keys) {
            newProps.put(key, getProperty(key));
        }
        clear();
        newProps.values().forEach(p -> put(p.getKey(), p.getValue()));
    }

    public Object putFirst(String key, Object value){
        JSProperty oldProp = getProperty(key);
        remove(key);

        LinkedHashMap<String, JSProperty> oldMap = properties;
        properties = new LinkedHashMap<>();

        put(key, value);
        for(JSProperty prop : oldMap.values()){
            putProperty(prop);
        }
        return oldProp != null ? oldProp.getValue() : null;
    }

}
