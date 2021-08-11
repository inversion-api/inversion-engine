package io.inversion;

import io.inversion.utils.Path;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class EndpointTest {


    @Test
    public void test_mergePaths() {

        merge("[a/*]", "a/*", "a");
        merge("[a/*]", "a", "a/*");

        merge("[a/b]", "a/*", "a/b");
        merge("[a/b]", "a/b", "a/*");
        merge("[a, a/b]", "a", "a/b");

        merge("[a, a/b/c]", "a", "a/b/c");
        merge("[a, a/b/c]", "a/b/c", "a");


        merge("[a/b/c]", "a/*", "a/b/c");
        merge("[a/b/c]", "a/b/c", "a/*");

        merge("[a/b/c/*]", "a/*", "a/b/c/*");
        merge("[a/b/c/*]", "a/b/c/*", "a/*");

        merge("[a/b, a/b/c]", "a/b", "a/b/c");
        merge("[a/b]", "a/{asdf}", "a/b");
        merge("[a/b, a/b/c]", "a/{asdf}", "a/b/c");


        merge("[a/b, a/b/c]", "a/b/c", "a/{asdf}");

        merge("[a/b/c]", "a/b/c", "a/{asdf}/*");
        merge("[a/b, a/b/c/*]", "a/b/c/*", "a/{asdf}");


        merge("[1/2/3/4]", "{v1}/{v1}/{v1}/{v1}", "1/{a}/{b}/{c}/", "{a}/2/{b}/{c}/", "{a}/{b}/3/{c}/", "{a}/{b}/{c}/4");
        merge("[1/2/3/4]", "{v1}/{v1}/{v1}/{v1}", "1/*", "{a}/2/*", "{a}/{b}/3/*", "{a}/{b}/{c}/4");


         merge("[1/2/3/4]","{v1}/{v1}/{v1}/{v1}", "1/*", "{a}/2/3/4");

    }

    static void merge(String expected, String... paths) {
        List<Path> list = new ArrayList<>();
        for (String p : paths) {
            list.add(new Path(p));
        }
        List<Path> merged = new ArrayList();
        Endpoint.mergePaths(merged, list);
        System.out.println("MERGING: " + expected + " vs " + merged + " - " + list);
        assertEquals(expected, merged.toString());
    }


//    @Test
//    public void test_getOperationPaths_wildcard_ep_path() {
//        Api      api = new Api();
//        Endpoint ep  = new Endpoint().withIncludeOn("GET", "*");
//        Action   a1  = new Action<Action>().withIncludeOn("{collection}/[{resource}]/[{relationship}]/");
//        ep.withActions(a1);
//
//        ArrayListValuedHashMap<String, Path> paths = ep.getOperationPaths(api, null);
//        for (String method : paths.keySet()) {
//            System.out.println(method + " - " + paths.get(method));
//        }
//        assertEquals("[{collection}, {collection}/{resource}, {collection}/{resource}/{relationship}]", paths.get("GET").toString());
//    }
//
//    @Test
//    public void test_getOperationPaths_wildcard_action_path() {
//        Api      api = new Api();
//        Endpoint ep  = new Endpoint().withIncludeOn("GET", "some/path/*");
//        Action   a1  = new Action<Action>();
//        ep.withActions(a1);
//
//        ArrayListValuedHashMap<String, Path> paths = ep.getOperationPaths(api, null);
//        for (String method : paths.keySet()) {
//            System.out.println(method + " - " + paths.get(method));
//        }
//        assertEquals("[some/path]", paths.get("GET").toString());
//    }
//
//    @Test
//    public void test_getOperationPaths_ep_optional_path() {
//        Api      api = new Api();
//        Endpoint ep  = new Endpoint().withIncludeOn("GET", "a/[b]/*");
//        Action   a1  = new Action<Action>();
//        ep.withActions(a1);
//
//        ArrayListValuedHashMap<String, Path> paths = ep.getOperationPaths(api, null);
//        for (String method : paths.keySet()) {
//            System.out.println(method + " - " + paths.get(method));
//        }
//        assertEquals("[a, a/b]", paths.get("GET").toString());
//    }
//
//
//    @Test
//    public void test_getOperationPaths_ep_optional_path_overlapping_actions() {
//        Api      api = new Api();
//        Endpoint ep  = new Endpoint().withIncludeOn("GET", "a/[b]/c/[d]/e/*");
//        Action   a1  = new Action<Action>().withIncludeOn("GET", "b");
//        ep.withActions(a1);
//
//        ArrayListValuedHashMap<String, Path> paths = ep.getOperationPaths(api, null);
//        for (String method : paths.keySet()) {
//            System.out.println(method + " - " + paths.get(method));
//        }
//        assertEquals("[a/b]", paths.get("GET").toString());
//    }
//
//    @Test
//    public void test_getOperationPaths_ep_optional_path_overlapping_actions2() {
//        Api      api = new Api();
//        Endpoint ep  = new Endpoint().withIncludeOn("GET", "a/[b]/c/[d]/*");
//        Action   a1  = new Action<Action>().withIncludeOn("GET", "b/*");
//        //Action a2 = new Action<Action>().withIncludeOn("GET", "b/c");
//        ep.withActions(a1);
//
//        ArrayListValuedHashMap<String, Path> paths = ep.getOperationPaths(api, null);
//        for (String method : paths.keySet()) {
//            System.out.println(method + " - " + paths.get(method));
//        }
//        assertEquals("[a/b/*]", paths.get("GET").toString());
//    }
//
//    @Test
//    public void test_getOperationPaths_ep_optional_path_overlapping_actions3() {
//        Api      api = new Api();
//        Endpoint ep  = new Endpoint().withIncludeOn("GET", "a/[b]/c/d/e/*");
//        //Action a1 = new Action<Action>().withIncludeOn("GET", "b/*");
//        Action a2 = new Action<Action>().withIncludeOn("GET", "[b]/c/[d]/e/*");
//
//        //ep.withActions(a1, a2);
//        ep.withActions(a2);
//
//        ArrayListValuedHashMap<String, Path> paths = ep.getOperationPaths(api, null);
//        for (String method : paths.keySet()) {
//            System.out.println(method + " - " + paths.get(method));
//        }
//        assertEquals("[a, a/b/c, a/b/c/d/e/*]", paths.get("GET").toString());
//    }
//
//    @Test
//    public void test_getOperationPaths_no_matching_actions() {
//        Api      api = new Api();
//        Endpoint ep  = new Endpoint().withIncludeOn("GET", "a/[b]/c/[d]/*");
//        Action   a1  = new Action<Action>().withIncludeOn("GET", "b/[d]");
//        //Action a2 = new Action<Action>().withIncludeOn("GET", "b/c");
//        ep.withActions(a1);
//
//        ArrayListValuedHashMap<String, Path> paths = ep.getOperationPaths(api, null);
//        for (String method : paths.keySet()) {
//            System.out.println(method + " - " + paths.get(method));
//        }
//        assertEquals(0, paths.size());
//    }
//
//
//    @Test
//    public void test_getOperationPaths() {
//        Api      api = new Api();
//        Endpoint ep  = new Endpoint().withIncludeOn("*", "1/2/3/*");
//        Action   a1  = new Action<Action>().withIncludeOn("{collection}/[{resource}]/[{relationship}]/");
//        Action a2 = new Action<Action>().withIncludeOn("GET,POST", "something/{collection}/[{resource}]/[{relationship}]/");
//        Action a3 = new Action<Action>().withIncludeOn("GET,POST", "something/something/{collection}/[{resource}]/[{relationship}]/");
//
//        ep.withActions(a1, a2, a3);
//
//        ArrayListValuedHashMap<String, Path> paths = ep.getOperationPaths(api, null);
//
//        for (String method : paths.keySet()) {
//            System.out.println(method + " - " + paths.get(method));
//        }
//    }
//
//    @Test
//    public void test_getOperationPaths_conflicting_paths_of_same_size() {
//        Api      api = new Api();
//        Endpoint ep  = new Endpoint().withIncludeOn("GET", "1/2/3/*");
//        Action   a1  = new Action<Action>().withIncludeOn("GET", "a/b/c");
//        Action   a2  = new Action<Action>().withIncludeOn("GET", "d/e/f");
//        ep.withActions(a1, a2);
//        boolean thrown = false;
//        try {
//            ArrayListValuedHashMap<String, Path> paths = ep.getOperationPaths(api, null);
//        }
//        catch(ApiException ex){
//            thrown = true;
//        }
//        if(!thrown)
//            fail("An error should have been thrown because the paths are incompatible");
//    }
//
//    @Test
//    public void test_getOperationPaths_emptyActionPaths() {
//        Api      api = new Api();
//        Endpoint ep  = new Endpoint().withIncludeOn("GET", "1/2/3/*");
//        Action   a1  = new Action<Action>().withIncludeOn("*", "*");
//        ep.withActions(a1);
//        ArrayListValuedHashMap<String, Path> paths = ep.getOperationPaths(api, null);
//        for (String method : paths.keySet()) {
//            System.out.println(method + " - " + paths.get(method));
//        }
//
//        assertEquals("[1/2/3/a/b/c, 1/2/3/d/e/f]", paths.get("GET").toString());
//    }

}
