package io.inversion.json.jackson;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonAnyBean1 extends AnyBean {

    Map<String, Object> properties = new LinkedHashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }

    @JsonAnySetter
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }


    List<JsonAnyBean2> bean2s = new ArrayList();
    JsonAnyBean3       bean3  = null;
    String             str1   = null;
    int                int1   = 0;

    public List<JsonAnyBean2> getBean2s() {
        return bean2s;
    }

    public void setBean2s(List<JsonAnyBean2> bean2s) {
        this.bean2s = bean2s;
    }

    public JsonAnyBean3 getBean3() {
        return bean3;
    }

    public void setBean3(JsonAnyBean3 bean3) {
        this.bean3 = bean3;
    }

    public String getStr1() {
        return str1;
    }

    public void setStr1(String str1) {
        this.str1 = str1;
    }

    public int getInt1() {
        return int1;
    }

    public void setInt1(int int1) {
        this.int1 = int1;
    }
}
