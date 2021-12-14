package io.inversion.config;

import ioi.inversion.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bean1 {

    String name = null;

    transient String transientStr          = null;
    transient String defaultedTransientStr = "DEFAULTED_TRANSIENT_STRING";
    final     String finalString           = "FINAL_STRING_VALUE";
    String defaultedString = "DEFAULTED_STRING_VALUE";

    private String defaultedPrivateString = "DEFAULTED_PRIVATED_STRING";
    private String privateString          = null;

    int          int1                = 0;
    String       str1                = null;
    List<String> list1               = null;
    List<String> defaultedStringList = Utils.asList("str1", "str2");

    Map<String, Bean1> bean2Map         = new HashMap<>();
    Map<String, Bean2> defaultedBean2Map = Utils.asMap("bean1", new Bean2(), "bean2", new Bean2());

    List<Bean2> bean2List = new ArrayList<>();

    Bean2 defaultedBean2 = new Bean2();
    Bean1 bean1 = null;

    public String getName() {
        return name;
    }

    public Bean1 withName(String name) {
        this.name = name;
        return this;
    }

    public String getTransientStr() {
        return transientStr;
    }

    public Bean1 withTransientStr(String transientStr) {
        this.transientStr = transientStr;
        return this;
    }

    public String getDefaultedTransientStr() {
        return defaultedTransientStr;
    }

    public Bean1 withDefaultedTransientStr(String defaultedTransientStr) {
        this.defaultedTransientStr = defaultedTransientStr;
        return this;
    }

    public String getFinalString() {
        return finalString;
    }

    public String getDefaultedString() {
        return defaultedString;
    }

    public Bean1 withDefaultedString(String defaultedString) {
        this.defaultedString = defaultedString;
        return this;
    }

    public String getDefaultedPrivateString() {
        return defaultedPrivateString;
    }

    public Bean1 withDefaultedPrivateString(String defaultedPrivateString) {
        this.defaultedPrivateString = defaultedPrivateString;
        return this;
    }

    public String getPrivateString() {
        return privateString;
    }

    public Bean1 withPrivateString(String privateString) {
        this.privateString = privateString;
        return this;
    }

    public int getInt1() {
        return int1;
    }

    public Bean1 withInt1(int int1) {
        this.int1 = int1;
        return this;
    }

    public String getStr1() {
        return str1;
    }

    public Bean1 withStr1(String str1) {
        this.str1 = str1;
        return this;
    }

    public List<String> getList1() {
        return list1;
    }

    public Bean1 withList1(List<String> list1) {
        this.list1 = list1;
        return this;
    }

    public List<String> getDefaultedStringList() {
        return defaultedStringList;
    }

    public Bean1 withDefaultedStringList(List<String> defaultedStringList) {
        this.defaultedStringList = defaultedStringList;
        return this;
    }

    public Map<String, Bean1> getBean2Map() {
        return bean2Map;
    }

    public Bean1 withBean2Map(Map<String, Bean1> bean2Map) {
        this.bean2Map = bean2Map;
        return this;
    }

    public Map<String, Bean2> getDefaultedBean2Map() {
        return defaultedBean2Map;
    }

    public Bean1 withDefaultedBean2Map(Map<String, Bean2> defaultedBean2Map) {
        this.defaultedBean2Map = defaultedBean2Map;
        return this;
    }

    public Bean2 getDefaultedBean2() {
        return defaultedBean2;
    }

    public Bean1 withDefaultedBean2(Bean2 defaultedBean2) {
        this.defaultedBean2 = defaultedBean2;
        return this;
    }

    public Bean1 getBean1() {
        return bean1;
    }

    public Bean1 withBean1(Bean1 bean1) {
        this.bean1 = bean1;
        return this;
    }
}
