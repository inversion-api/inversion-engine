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

//    /**
//     * @param key the name to set
//     */
//    public void setKey(Object key) {
//        this.key = key;
//    }

    /**
     * @return the value
     */
    public Object getValue() {
        return value;
    }

//    /**
//     * @param value the value to set
//     */
//    public void setValue(Object value) {
//        this.value = value;
//    }
}
