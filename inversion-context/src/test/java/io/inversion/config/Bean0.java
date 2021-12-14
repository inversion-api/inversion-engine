package io.inversion.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Bean0 {

    String name = null;
    String var1 = null;

    ArrayList<String> stringsList = new ArrayList<>();
    HashMap<String, String> stringsMap = new HashMap();

    ArrayList<Bean1> bean1List = new ArrayList();
    HashMap<Bean1, Bean2> bean1Bean2Map = new HashMap();
    HashMap<String, Bean2> stringBean0Map = new HashMap();
    HashMap<Bean2, String> bean0StringMap = new HashMap();

    public String getName() {
        return name;
    }

    public Bean0 withName(String name) {
        this.name = name;
        return this;
    }

    public String getVar1() {
        return var1;
    }

    public Bean0 withVar1(String var1) {
        this.var1 = var1;
        return this;
    }

    public ArrayList<String> getStringsList() {
        return stringsList;
    }

    public Bean0 withStringsList(ArrayList<String> stringsList) {
        this.stringsList = stringsList;
        return this;
    }

    public HashMap<String, String> getStringsMap() {
        return stringsMap;
    }

    public Bean0 withStringsMap(HashMap<String, String> stringsMap) {
        this.stringsMap = stringsMap;
        return this;
    }

    public ArrayList<Bean1> getBean1List() {
        return bean1List;
    }

    public Bean0 withBean1List(ArrayList<Bean1> bean1List) {
        this.bean1List = bean1List;
        return this;
    }

    public HashMap<Bean1, Bean2> getBean1Bean2Map() {
        return bean1Bean2Map;
    }

    public Bean0 withBean1Bean2Map(HashMap<Bean1, Bean2> bean1Bean2Map) {
        this.bean1Bean2Map = bean1Bean2Map;
        return this;
    }

    public HashMap<String, Bean2> getStringBean0Map() {
        return stringBean0Map;
    }

    public Bean0 withStringBean0Map(HashMap<String, Bean2> stringBean0Map) {
        this.stringBean0Map = stringBean0Map;
        return this;
    }

    public HashMap<Bean2, String> getBean0StringMap() {
        return bean0StringMap;
    }

    public Bean0 withBean0StringMap(HashMap<Bean2, String> bean0StringMap) {
        this.bean0StringMap = bean0StringMap;
        return this;
    }
}
