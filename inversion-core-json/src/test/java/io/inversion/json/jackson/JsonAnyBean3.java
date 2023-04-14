package io.inversion.json.jackson;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.LinkedHashMap;
import java.util.Map;

public class JsonAnyBean3 {

    Map<String, Object> properties = new LinkedHashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }

    @JsonAnySetter
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }


    String asdf = null;
    int qwer = 0;

    public String getAsdf() {
        return asdf;
    }

    public void setAsdf(String asdf) {
        this.asdf = asdf;
    }

    public int getQwer() {
        return qwer;
    }

    public void setQwer(int qwer) {
        this.qwer = qwer;
    }
}
