package io.inversion.json;

import io.inversion.utils.Utils;

public interface JSGet {

    JSNode getJson();

    default JSNode getNode(Object key) throws ClassCastException {
        return (JSNode) getJson().get(key);
    }

    default JSMap getMap(Object key) throws ClassCastException {
        return (JSMap) getJson().get(key);
    }

    /**
     * Convenience overloading of {@link JSNode#get(Object)}
     *
     * @param key the case insensitive property name or index.
     * @return the value of property <code>name</code> cast to a JSList if exists else null
     * @throws ClassCastException if the object found is not a JSList
     */
    default JSList getList(Object key) {
        return (JSList) getJson().get(key);
    }

    /**
     * Convenience overloading of {@link JSNode#get(Object)}
     *
     * @param key the case insensitive property name to retrieve.
     * @return the stringified value of property <code>name</code> if it exists else null
     */
    default String getString(Object key) {
        Object value = getJson().get(key);
        if (value != null)
            return value.toString();
        return null;
    }

    /**
     * Convenience overloading of {@link JSNode#get(Object)}
     *
     * @param key the case insensitive property name to retrieve.
     * @return the value of property <code>name</code> stringified and parsed as an int if it exists else -1
     */
    default int getInt(Object key) {
        Object found = getJson().get(key);
        if (found != null)
            return Utils.atoi(found);

        return -1;
    }

    /**
     * Convenience overloading of {@link JSNode#get(Object)}
     *
     * @param key the case insensitive property name to retrieve.
     * @return the value of property <code>name</code> stringified and parsed as long if it exists else -1
     */
    default long getLong(Object key) {
        Object found = getJson().get(key);
        if (found != null)
            return Utils.atol(found);

        return -1;
    }

    /**
     * Convenience overloading of {@link JSNode#get(Object)}
     *
     * @param key the case insensitive property name to retrieve.
     * @return the value of property <code>name</code> stringified and parsed as a double if it exists else -1
     */
    default double getDouble(Object key) {
        Object found = getJson().get(key);
        if (found != null)
            return Utils.atod(found);

        return -1;
    }

    /**
     * Convenience overloading of {@link JSNode#get(Object)}
     *
     * @param key the case insensitive property name to retrieve.
     * @return the value of property <code>name</code> stringified and parsed as a boolean if it exists else false
     */
    default boolean getBoolean(Object key) {
        Object found = getJson().get(key);
        if (found != null)
            return Utils.atob(found);

        return false;
    }

}
