/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion;

import io.inversion.utils.Utils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
public class UrlTest {


    @Test
    public void test_relative_protocol(){
        Url u = new Url("//somehost.com/path");
        assertEquals("//", u.getProtocol());
        assertTrue(u.getPort() <= 0);
        assertEquals("somehost.com", u.getHost());
        assertEquals("//somehost.com/path", u.toString());
    }

    @Test
    public void test_pass_parsing(){

        passUrl("http://asdf?offset=5", "http://asdf?offset=5");

        passUrl("http://localhost:8080?someField1=value1", "http://localhost:8080?someField1=value1");


        passUrl("https://some.my.host/path/v1/{var1}/{var2}", "https://some.my.host/path/v1/{var1}/{var2}/", "remove trailing slash");
        passUrl("//127.0.0.1:8080", "/", "default host with no trailing slash");

        passUrl("//127.0.0.1:8080/path", "path/");
        passUrl("//127.0.0.1:8080/path", "/path");
        passUrl("//127.0.0.1:8080/path", "/path/");

        passUrl("//127.0.0.1:8080/path", "path///");

        passUrl("http://127.0.0.1:8080/a/b", "http://127.0.0.1:8080////a///b///");
        passUrl("https://127.0.0.1:8080/a/b", "https://127.0.0.1:8080////a///b///");
        passUrl("http://127.0.0.1/a/b", "http://127.0.0.1////a///b///");

        passUrl("//127.0.0.1:8080/{asdf...asdf}", "{asdf...asdf}");

        //passUrl("http://host:8080/abc", "http:/host:8080/abc");
        //passUrl("http://host:8080/abc", "http://///host:8080////abc");
        //passUrl("http://host:8080/abc", "http:host:8080////abc");

        passUrl("//127.0.0.1:8080", "");
    }

//    @Test
//    public void test_server_host_var(){
//        Url url = new Url("http://{host}:8080");
//        assertEquals("{host}", url.getHost());
//        assertEquals(8080, url.getPort());
//    }

    @Test
    public void test_should_fail_parsing(){
//
//        //failUrl("////");
//        failUrl( "///path");
//        failUrl("///path///");
//
//        failUrl( "////a///b///");
//
//        failUrl(".");
//        failUrl("..");
//        failUrl("./");
//        failUrl("../");
//
//        failUrl("http:/host/.");
//        failUrl("http:/host/./");
//        failUrl("http:/host/..");
//        failUrl("http:/host/../");
//
//        failUrl("http:/host:8080/.");
//        failUrl("http:/host:8080/./");
//        failUrl("http:/host:8080/..");
//        failUrl("http:/host:8080/../");
//
//        failUrl("http:/host:abc");
//
//        failUrl("./");
//        failUrl("/.");
//        failUrl("asf/../sd");
//        failUrl("/../sd");
    }

    void failUrl(String url){
        failUrl(url, null);
    }
    void failUrl(String url, String comment){
        Url u = null;
        try{
            u = new Url(url);
        }
        catch(Exception ex){
            return;
        }
        fail("SHOULD FAIL: '" + url + "' parsed as '" + u + "'" + (comment != null ? (" - " + comment) : ""));
    }

    void passUrl(String expected, String url){
        assertEquals(expected, new Url(url).toString());
    }

    void passUrl(String expected, String url, String comment){
        Url u = new Url(url);
        String str = u.toString();
        assertEquals(expected, str, comment);
    }

    @Test
    public void testUrlWithParams() {
        assertEquals("http://test.com/api?a=b&c=d", new Url("http://test.com/api").withParams("a", "b", "c", "d").toString());
        assertEquals("http://test.com/api?a=b&c=d&e", new Url("http://test.com/api").withParams("a", "b", "c", "d", "e").toString());
    }

//    @Test
//    public void test_preserve_param_order() {
//        String[] tests = new String[]{"http://host.com?zzz=zzz&aaa=aaa&111=111&333=333"};
//
//        for (String test : tests) {
//            Url    url    = new Url(test);
//            String output = url.toString();
//
//            if (!test.equals(output)) {
//                System.out.println(Utils.parseQueryString(Utils.substringAfter(test, "?")));
//                System.out.println(url.getParams());
//
//                System.out.println("EXPECTED: " + test);
//                System.out.println("FOUND   : " + output);
//                fail();
//            }
//        }
//        //      assertEquals("http://test.com/api?a=b&c=d", new Url("http://test.com/api").withParams("a", "b", "c", "d").toString());
//        //      assertEquals("http://test.com/api?a=b&c=d&e", new Url("http://test.com/api").withParams("a", "b", "c", "d", "e").toString());
//    }

}
