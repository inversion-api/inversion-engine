package io.inversion.json;

import io.inversion.utils.Utils;
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.util.*;

public class JSMap extends LinkedCaseInsensitiveMap implements JSNode {


    /**
     * Creates an empty JSNode.
     */
    public JSMap() {

    }

    public JSMap(Map map) {
        putAll(map);
    }

    /**
     * Creates a JSNode with <code>nameValuePairs</code> as the initial properties.
     * <p>
     * The first and every other element in <code>nameValuePairs</code> should be a string.
     *
     * @param nameValuePairs the name value pairs to add
     */
    public JSMap(Object... nameValuePairs) {
        putAll(Utils.asMap(nameValuePairs));
    }

    public Object put(Object key, Object value){
        if(key == null)
            return null;

        values();
        return put(key.toString(), value);
    }

    @Override
    public Object removeProp(Object key) {
        return remove(key);
    }



    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //-- Map superclass methods required to override for compatibality

    /**
     * Override required to avoid concurrent modification exception
     * @return
     */
    @Override
    public Object get(Object obj) {
        if(obj == null)
            return null;
        if(!(obj instanceof String))
            obj = obj.toString();

        return super.get(obj);
    }



    /**
     * Override required to avoid concurrent modification exception
     * @return
     */
    @Override
    public Set<String> keySet() {
        return super.keySet();
    }

    /**
     * Override required to convert keys to strings
     * @param map
     */
    @Override
    public void putAll(Map map){
        for(Object key : map.keySet()){
            if(key != null){
                put(key.toString(), map.get(key));
            }
        }
    }


    public JSMap with(Object key, Object value, Object... nvPairs){
        put(key, value, nvPairs);
        return this;
    }


    /**
     * @return json string, pretty printed with properties written out in their original case.
     */
    @Override
    public String toString() {
        return JSWriter.toJson(this, true, false);
    }

    @Override
    public int hashCode(){
        return System.identityHashCode(this);
    }


    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //-- Utility Methods



    /**
     * Changes the property name iteration order from insertion order to alphabetic order.
     */
    public JSMap sort() {
        LinkedHashMap<String, Object> copy = new LinkedHashMap(this);

        List<String> sortedKeys = new ArrayList(keySet());
        Collections.sort(sortedKeys);
        clear();
        sortedKeys.forEach(k -> put(k, copy.get(k)));
        return this;
    }

    public Object putFirst(String key, Object value){
        Object oldValue = get(key);
        remove(key);

        LinkedHashMap<String, Object> copy = new LinkedHashMap<>(this);
        clear();
        put(key, value);
        putAll(copy);

        return oldValue;
    }


}
