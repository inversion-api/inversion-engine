package io.inversion.config;

import ioi.inversion.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestContext2 {

    Bean1 getBean1(String name){
        Bean1 b = new Bean1();
        b.name = name;
        b.str1 = "hello world";
        b.withPrivateString("private!!!");
        b.withDefaultedTransientStr("transient!!!");
        return b;
    }

    @Test
    public void test_encode_string_list(){
        Bean0 bean = new Bean0();
        Utils.add(bean.stringsList, "red", "black");
        Context ctx = new Context();
        Map<String, String> found = ctx.encode(new HashMap<>(), bean);
        assertEquals(Utils.asMap("_anonymous_Bean0_1.stringsList", "[red, black]"), found);
    }

    @Test
    public void test_encode_string_map(){
        Bean0 bean = new Bean0();
        Utils.addToMap(bean.stringsMap, "red", "black", "blue", "green");
        Context ctx = new Context();
        Map<String, String> found = ctx.encode(new HashMap<>(), bean);
        assertEquals(Utils.asMap("_anonymous_Bean0_1.stringsMap", "{blue=green, red=black}"), found);
    }

    @Test
    public void test_encode_stringToBean_map(){
        Bean0 bean = new Bean0();
        bean.stringBean0Map.put("red", new Bean2().withName("bean2_red"));
        bean.stringBean0Map.put("black", new Bean2().withName("bean2_black"));
        Context ctx = new Context();
        Map<String, String> found = ctx.encode(new HashMap<>(), bean);
        assertEquals(Utils.asMap("_anonymous_Bean0_1.stringBean0Map", "{black=bean2_black, red=bean2_red}"), found);
    }


    void assertEncoded(Map<String, String> expected, Map<String, String> actual){
        assertEquals(expected.size(), actual.size());
        for(String key : expected.keySet()){
            assertEquals(expected.get(key), actual.get(key));
        }
    }

    @Test
    public void test_wire(){
        Context ctx = new Context();
        Bean1 bean1 = getBean1("bean1");
        Map<String, String> props = new HashMap<>();
        Map<String, String> encoded = ctx.wire(props, bean1);
        //TODO: check return values...some defaults are getting encoded
    }

    @Test
    public void test_wire_passing_same_object_more_than_once_OK(){
        Context ctx = new Context();
        Bean1 bean1 = getBean1("bean1");
        Map<String, String> props = new HashMap<>();
        Map<String, String> encoded = ctx.wire(props, bean1, bean1);
        //TODO: check return values...some defaults are getting encoded
    }

    @Test
    public void test_wire_self_loop_OK(){
        Context ctx = new Context();
        Bean1 bean1 = getBean1("bean1");
        bean1.withBean1(bean1);
        Map<String, String> props = new HashMap<>();
        Map<String, String> encoded = ctx.wire(props, bean1);
        //TODO: check return values...some defaults are getting encoded
    }

    @Test
    public void test_wire_multiple_loop_OK(){
        Context ctx = new Context();
        Bean1 bean1 = getBean1("bean1");
        bean1.withBean1(bean1);

        Bean2 bean2 = new Bean2();
        bean2.bean1 = bean1;

        Bean2 bean2_2 = new Bean2();
        bean2_2.bean1 = bean1;

        bean2.bean2 = bean2_2;

        bean1.bean2List.add(bean2);
        bean1.bean2List.add(bean2_2);

        Map<String, String> props = new HashMap<>();
        Map<String, String> encoded = ctx.wire(props, bean1, bean2);
        //TODO: check return values...some defaults are getting encoded
    }

}
