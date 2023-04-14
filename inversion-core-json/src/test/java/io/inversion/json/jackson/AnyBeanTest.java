package io.inversion.json.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URL;

public class AnyBeanTest {

    ObjectMapper mapper = new ObjectMapper();

//    @Test
//    public void test_jackson_decoding_1() throws Exception {
//        URL          url      = getClass().getResource("anybean_test1.json");
//        JsonAnyBean1 anybean1 = mapper.readValue(url, JsonAnyBean1.class);
//        ReflectionToStringBuilder.toString(anybean1);
//        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(anybean1);
//        System.out.println(json);
//
//    }
//

//    @Test
//    public void test_jackson_jsnode_encoding_1() throws Exception {
//
//        URL    url  = getClass().getResource("jackson_jsnode_encoding_1.json");
//        String json = Utils.read(url.openStream());
//        JSNode node = (JSNode) JSReader.parseJson(json);
//
//        JSNode jacksonNode = mapper.readValue(url, JSNode.class);
//
//        System.out.println(jacksonNode);
//    }
//
//    @Test
//    public void test_jackson_decoding_1() throws Exception {
//        URL    url  = getClass().getResource("order1.json");
//        Order order = mapper.readValue(url, Order.class);
//        System.out.println(order);
//        String json = mapper.writeValueAsString(order);
//        System.out.println(json);
//    }
//
//    @Test
//    public void test_jackson_jsnode_decoding_1() throws Exception {
//
//        URL    url  = getClass().getResource("order1.json");
//        String json = Utils.read(url.openStream());
//        JSNode node = (JSNode) JSReader.parseJson(json);
//
//        OrderNode jacksonNode = mapper.readValue(url, OrderNode.class);
//
//        System.out.println(jacksonNode);
//    }
}
