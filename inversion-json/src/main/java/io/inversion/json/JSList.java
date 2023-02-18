package io.inversion.json;


import io.inversion.utils.Utils;

import java.util.*;

public class JSList extends ArrayList implements JSNode {

    JSListKeys keySet = null;

    public JSList(Object... objects) {
        if (objects != null && objects.length == 1 && objects[0].getClass().isArray()) {
            objects = (Object[]) objects[0];
        } else if (objects != null && objects.length == 1 && java.util.Collection.class.isAssignableFrom(objects[0].getClass())) {
            objects = ((java.util.Collection) objects[0]).toArray();
        }

        for (int i = 0; objects != null && i < objects.length; i++)
            add(objects[i]);
    }


    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //-- JSNode Implementation


    @Override
    public Object get(Object key) {
        int idx = Utils.atoi(key);
        if (idx > -1 && idx < size())
            return get(idx);
        return null;
    }

    @Override
    public Object put(Object key, Object value) {
        int idx = Utils.atoi(key);
        return set(idx, value);
    }

    @Override
    public Object removeProp(Object key) {
        int idx = Utils.atoi(key);
        if (idx > -1 && idx < size()) {
            Object oldVal = get(idx);
            remove(idx);
            return oldVal;
        }
        return null;
    }


    /**
     * Returns this JSList
     */
    @Override
    public List<Object> values() {
        return this;
    }

    /**
     * @return a Set backed by the index numbers of this list
     */
    @Override
    public Set<String> keySet() {
        if(keySet == null)
            keySet = new JSListKeys();
        return keySet;
    }

    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //-- Additional Overrides required for compatibility

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

    /**
     * Overridden to automatically expand the size of the list to accept idx
     * @param idx
     * @param value
     * @return
     */
    @Override
    public Object set(int idx, Object value){
        if (idx >= 0) {
            while (size() <= idx)
                add(null);

            Object oldVal = get(idx);
            super.set(idx, value);
            return oldVal;
        }
        return null;
    }

    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //-- Additional Utilities

    public Object last() {
        if (size() > 0)
            return get(size() - 1);
        return null;
    }

//    public JSMap lastMap() {
//        if (size() > 0)
//            return (JSMap) get(size() - 1);
//        return null;
//    }

//    public T lastAs(Class<T> type) {
//        if (size() > 0) {
//            try {
//                return mapper.readValue(get(size() - 1).toString(), type);
//            } catch (Exception ex) {
//                Utils.rethrow(ex);
//            }
//        }
//        return null;
//    }
//
//    public List<T> as(Class<T> type) {
//        try {
//            return mapper.readValue(toString(), mapper.getTypeFactory().constructCollectionType(List.class, type));
//        } catch (Exception ex) {
//            Utils.rethrow(ex);
//        }
//        return null;
//
//    }

    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //-- Helper Classes

    class JSListKeys extends AbstractSet{

        @Override
        public Iterator iterator() {
            return new JSListKeyIterator();
        }

        @Override
        public int size() {
            return JSList.this.size();
        }
    }

    class JSListKeyIterator implements Iterator<String>{

        int expectedSize = JSList.this.size();
        int next = 0;

        @Override
        public boolean hasNext() {
            int size = JSList.this.size();
            if(expectedSize != size)
                throw new ConcurrentModificationException();
            return next < size;
        }

        @Override
        public String next() {
            String nextStr = ((Integer)next).toString();
            next += 1;
            return nextStr;
        }
    }
}

