package io.inversion.config;

public class Bean2 {
    String name = null;
    String str1 = null;
    Bean1 bean1 = null;
    Bean2 bean2 = null;

    public String getName() {
        return name;
    }

    public Bean2 withName(String name) {
        this.name = name;
        return this;
    }

    public String getStr1() {
        return str1;
    }

    public Bean2 withStr1(String str1) {
        this.str1 = str1;
        return this;
    }

    public Bean1 getBean1() {
        return bean1;
    }

    public Bean2 withBean1(Bean1 bean1) {
        this.bean1 = bean1;
        return this;
    }

    public Bean2 getBean2() {
        return bean2;
    }

    public Bean2 withBean2(Bean2 bean2) {
        this.bean2 = bean2;
        return this;
    }
}
