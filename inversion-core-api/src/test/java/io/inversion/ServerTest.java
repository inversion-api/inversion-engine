package io.inversion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ServerTest {

//    @Test
//    public void lazyConfig_conflictingPathParams_throwsException() {
//
//        try {
//            Server s = new Server("http://127.0.0.1/dev/{version}/{tenant}/*",
//                    "http://127.0.0.1/stage/northwind/{tenant}/{version}/*");
//            s.afterWiringComplete(null);
//            fail("should have thrown an exception because the path variables don't match");
//        } catch (ApiException ex) {
//            //OK
//        }
//    }
//
//    @Test
//    public void lazyConfig_conflictingHostParams_throwsException() {
//
//        try {
//            Server s = new Server("http://{tenant}.api.com/dev/{version}/{tenant}/*",
//                    "http://api.{tenant}.com/prod/{version}/{tenant}/*");
//            s.afterWiringComplete(null);
//            fail("should have thrown an exception because the path variables don't match");
//        } catch (ApiException ex) {
//            //OK
//        }
//    }
//
//    @Test
//    public void lazyConfig_multipleCompatibleUrlsAndPaths() {
//
//        Server s = new Server("http://{tenant}.dev.api.com/dev/{version}/{tenant}/*",
//                "http://{tenant}.stage.api.com/stage/{version}/{tenant}/*",
//                "http://{tenant}.api.com/api/{version}/{tenant}/*");
//        s.afterWiringComplete(null);
//
//        assertEquals("[http://{tenant}.dev.api.com, http://{tenant}.stage.api.com, http://{tenant}.api.com]", s.getUrls().toString());
//        assertEquals("[dev/{version}/{tenant}/*, stage/{version}/{tenant}/*, api/{version}/{tenant}/*]", s.getAllIncludePaths().toString());
//    }
//
//
//    @Test
//    public void lazyConfig_mergeUrlPaths() {
//
//        Server server = new Server("http://localhost/urlPath")
//                .withIncludeOn("GET,path1/path1.1,path2/*")
//                .withIncludeOn("POST,/path3/*")
//                .withExcludeOn("PUT,path2/excluded/*")
//                .withExcludeOn("POST,path3/excluded/*");
//
//
//        server.checkLazyConfig();//doLazyConfig();//lazyConfig_mergeUrlPaths();
//
//        String actualIncludes = "[[GET,urlPath/path1/path1.1,urlPath/path2/*], [POST,urlPath/path3/*]]";
//                                //""
//        String actualExcludes = "[[PUT,urlPath/path2/excluded/*], [POST,urlPath/path3/excluded/*]]";
//
//        assertEquals(actualIncludes, server.getIncludeMatchers().toString());
//        assertEquals(actualExcludes, server.getExcludeMatchers().toString());
//    }
//
//    @Test
//    public void lazyConfig_removeUrlPaths() {
//        Server server = new Server("http://localhost/urlPath/toRemove",
//                "http://localhost:8080/urlPath/toRemove",
//                "https://localhost/urlPath/toRemove",
//                "*",
//                "some/path",
//                null);
//
//        server.checkLazyConfig();//lazyConfig_removeUrlPaths();
//
//        String expected = "[http://localhost, http://localhost:8080, https://localhost, *, some/path]";
//        String actual   = server.getUrls().toString();
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void lazyConfig_applyParams() {
//        Server server = new Server().withUrls("http://{host}:8080/",
//                "http://{host}.acme.com",
//                "https://{host}.secure_acme.com")
//                .withIncludeOn("dev/{tenant}")
//                .withIncludeOn("stage/{tenant}")
//                .withIncludeOn("prod/{tenant}");
//
//        server.checkLazyConfig();//lazyConfig_applyParams();
//        System.out.println(server.getParams());
//    }

}
