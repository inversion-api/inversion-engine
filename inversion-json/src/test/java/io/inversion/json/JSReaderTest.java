package io.inversion.json;

import io.inversion.utils.Utils;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JSReaderTest {

    @Test
    public void test_read() throws Exception {
        URL    url    = getClass().getResource("reader_test1.json");
        String source = Utils.read(url.openStream());
        JSNode read   = JSReader.asJSNode(source);

        JSList array  = read.getList("array");
        JSMap  node   = read.getMap("node");
        Object string = read.getString("string");

        assertEquals("a", array.get(0));
        assertEquals("b", array.get(1));
        assertEquals("c", array.get(2));

        assertEquals("value", string);
        assertEquals("world", node.getValue("hello"));
    }

    @Test
    public void test_read_write() throws Exception {

        for (int i = 1; i <= 100; i++) {
            String fileName = "reader_test" + i + ".json";
            URL    url      = getClass().getResource(fileName);
            if (url == null)
                continue;
            String source = Utils.read(url.openStream());

            JSNode read1  = JSReader.asJSNode(source);
            String write1 = read1.toString();
            JSNode read2  = JSReader.asJSNode(write1);
            String write2 = read2.toString();
            assertEquals(write1, write2);

            System.out.println(write2);
        }

    }
}
