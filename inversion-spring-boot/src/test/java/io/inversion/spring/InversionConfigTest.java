package io.inversion.spring;

import io.inversion.Api;
import io.inversion.Engine;
import io.inversion.spring.config.InversionConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InversionConfigTest {

    @Test
    public void buildServletMapping_noCommonPath() {
        Engine e = new Engine();
        e.withApi(new Api().withServers("/a/b/c", "/d/e/f"));
        assertEquals("/*", InversionConfig.buildServletMapping(e));
    }

    @Test
    public void buildServletMapping_partialCommonPath() {
        Engine e = new Engine();
        e.withApi(new Api().withServers("/a/b/c", "/a/b/f"));

        assertEquals("/a/b/*", InversionConfig.buildServletMapping(e));
    }

    //TODO: put back in
//    @Test
//    public void buildServletMapping_breakOnOptional() {
//        Engine e = new Engine();
//        e.withApi(new Api().withServer(new Server("/a/b/c,/a/b/[c]")));
//
//        assertEquals("/a/b/*", InversionServletConfig.buildServletMapping(e));
//    }
//
//    @Test
//    public void buildServletMapping_breakOnVariable() {
//        Engine e = new Engine();
//        e.withApi(new Api().withServer(new Server("/a/b/c,/a/b/{c}")));
//
//        assertEquals("/a/b/*", InversionServletConfig.buildServletMapping(e));
//    }

}
