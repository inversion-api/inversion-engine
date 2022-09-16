package io.inversion.json;

public class JSProperty {

    final Object key;
    final Object value;

    public JSProperty(Object key, Object value) {
        super();
        this.key = key;
        this.value = value;
    }

    public String toString() {
        return key + " = " + value;
    }

    /**
     * @return the name
     */
    public Object getKey() {
        return key;
    }

    /**
     * @return the value
     */
    public Object getValue() {
        return value;
    }

}
