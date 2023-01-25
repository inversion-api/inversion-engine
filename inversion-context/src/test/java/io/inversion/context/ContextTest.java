package io.inversion.context;

import io.inversion.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContextTest {

    public static Map<String, String> assertEncodeDecodeEncodeMatches(Object bean, String expected) {
        return assertEncodeDecodeEncodeMatches(new Context(), bean, expected);
    }

    public static Map<String, String> assertEncodeDecodeEncodeMatches(Context context, Object bean, String expected) {

        Map<String, String> initialEncodedProps = context.encode(bean);
        String              initialActual       = initialEncodedProps.toString();

        if (expected != null) {
            System.out.println("INITIAL EXPECTED: " + expected);
            System.out.println("INITIAL ACTUAL  : " + initialActual);
            assertEquals(expected, initialActual, "The initial model encoding did not match.");
        }

        String beanName = context.getName(bean);
        assertTrue(beanName != null);

        context.clear();

        context.decode(initialEncodedProps);
        Object decodedBean = context.getBean(beanName);

        context.clear();
        Map<String, String> reencodedProps = context.encode(decodedBean);
        String              finalActual    = reencodedProps.toString();

        Map<String, String> diffs = compareMaps(initialEncodedProps, reencodedProps);
        for (String key : diffs.keySet()) {
            System.out.println(key + " -> " + diffs.get(key));
        }
        assertTrue(diffs.size() == 0);

//        System.out.println("INITIAL EXPECTED: " + expected);
//        System.out.println("FINAL ACTUAL    : " + finalActual);
//        assertEquals(expected, finalActual, "After decode/encode cycle, the encoding did not match the expected.");

        return initialEncodedProps;
    }

    public static Map<String, String> compareMaps(Map<String, String> map1, Map<String, String> map2) {
        map1 = new LinkedHashMap(map1);
        map2 = new LinkedHashMap(map2);

        Map diffs = new LinkedHashMap();
        for (String key : map1.keySet()) {
            String value1 = map1.get(key);
            if (!map2.containsKey(key)) {
                diffs.put("MAP 2 MISSING: " + key, value1);
            } else {
                String value2 = map2.get(key);
                if (!Utils.equal(value1, value2)) {
                    diffs.put("DIFFERENCE   : " + key, value1 + ", " + value2);
                }
                map2.remove(key);
            }
        }
        for (String key : map2.keySet()) {
            String value2 = map2.get(key);
            diffs.put("MAP 1 MISSING: " + key, value2);
        }
        return diffs;
    }

    @Test
    public void test_bean_reference_loop_ok() {
        BeanWithLoop bean = new BeanWithLoop("bean", "value");
        assertEncodeDecodeEncodeMatches(bean, "{bean.class=io.inversion.context.BeanWithLoop, bean.property=value, bean.bean=bean}");
    }

    @Test
    public void test_bean_with_nested_reference_loop_ok() {
        BeanWithLoop parent = new BeanWithLoop("parent", "value");
        BeanWithLoop child  = new BeanWithLoop("child", "value");
        parent.bean = child;
        child.bean = parent;
        assertEncodeDecodeEncodeMatches(parent, "{parent.class=io.inversion.context.BeanWithLoop, parent.property=value, child.class=io.inversion.context.BeanWithLoop, child.property=value, child.bean=parent, parent.bean=child}");
    }

}
