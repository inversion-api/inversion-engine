package io.inversion;

public class Parameter extends Rule {

    String key = null;
    String in = null;
    boolean required = false;

    public Parameter(){

    }

    public Parameter(String name, String key, String in, boolean required){
        withName(name);
        withKey(key);
        withIn(in);
        withRequired(required);
    }

    public String getName() {
        return name;
    }

    public Parameter withName(String name) {
        this.name = name;
        return this;
    }

    public String getKey() {
        return key;
    }

    public Parameter withKey(String key) {
        this.key = key;
        return this;
    }

    public String getIn() {
        return in;
    }

    public Parameter withIn(String in) {
        this.in = in;
        return this;
    }

    public boolean isRequired() {
        return required;
    }

    public Parameter withRequired(boolean required) {
        this.required = required;
        return this;
    }
}
