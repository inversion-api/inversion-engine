package io.inversion;

import io.inversion.action.db.DbAction;
import io.inversion.action.hateoas.HALAction;
import io.inversion.action.misc.FileAction;
import io.inversion.action.misc.MockAction;
import io.inversion.action.openapi.OpenAPIAction;
import io.inversion.utils.Path;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ApiTest {


    void assertParameter(Op op, String key, int index){
        for(Param p : op.getParams()){
            if(key.equalsIgnoreCase(p.getKey()) && p.getIndex() == index)
                return;
        }
        fail("Parameter '" + key + "' at index " + index + " not found in operation");
    }

    void assertOp(List<Op> ops, String method, String displayPath){
        assertOp(ops, method, displayPath, -1);
    }

    void assertOp(List<Op> ops, String method, String displayPath, int numParams, Object... paramKeyIndexPairs){
        for(Op op : ops){
            if(op.getMethod().equalsIgnoreCase(method) && op.getPath().toString().equalsIgnoreCase(displayPath)){
                if(numParams > -1)
                    assertEquals(numParams, op.getParams().size());
                for(int i=0; paramKeyIndexPairs != null && i<paramKeyIndexPairs.length -1; i+=2){
                    assertParameter(op, (String)paramKeyIndexPairs[i], (Integer)paramKeyIndexPairs[i+1]);
                }
                return;
            }
        }
        fail("Operation " + method + " " + displayPath + " not found");
    }


    @Test
    public void test_rename_me(){
        DbAction dbAction  = new DbAction();
        HALAction        halAction = new HALAction();

        Api api = new Api()//
                .withDb(new BooksDb())
                .withIncludeOn("sipandsave/v2/{tenantCode}/*")//
                //-- TODO: this does not work but should
                .withEndpoint("[books]/*", halAction, dbAction)//
                .withEndpoint("[authors]/*", halAction, dbAction)//
                //.withEndpoint("openapi.json  ", new OpenAPIAction())
                //.withEndpoint("rapidoc.html", new FileAction())

                ;

        Engine e = new Engine(api);
        e.startup();
    }

    @Test
    public void test_buildRequestPaths_endpoint_identifies_full_path(){
        Api api = new Api()//
                .withEndpoint("GET,openapi.json,openapi.yaml", new MockAction())
                .withEndpoint("GET,rapidoc.html", new MockAction())
                ;
        assertPaths(api, "GET openapi.json", "GET openapi.yaml", "GET rapidoc.html");
    }

    @Test
    public void test_buildRequestPaths_action_identifies_full_path(){
        Api api = new Api()//
                .withEndpoint("GET", null, new MockAction().withIncludeOn("openapi.json,openapi.yaml"))
                .withEndpoint("GET, rapidoc.html", new MockAction())
                ;
        assertPaths(api, "GET openapi.json", "GET openapi.yaml", "GET rapidoc.html");
    }

    @Test
    public void test_buildRequestPaths_action_identifies_full_path_with_simple_regex(){
        Api api = new Api()//
                .withEndpoint("GET", null, new MockAction().withIncludeOn("{file:openapi}"))
                .withEndpoint("GET,rapidoc.html", new MockAction())
                ;
        assertPaths(api, "GET openapi.json", "GET openapi.yaml", "GET rapidoc.html");
    }

    void assertPaths(Api api, String... opMethodAndPathString){
        ArrayListValuedHashMap<String, Path> paths = api.buildRequestPaths();

        Set opStrings = new HashSet<>();
        for(String method : paths.keySet()){
            for(Path path : paths.get(method)){
                opStrings.add(method + " " + path);
            }
        }
        assertEquals(opMethodAndPathString.length, opStrings.size(), "The api produced the wrong number of operations");
        for(String str : opMethodAndPathString){
            assertTrue(opStrings.contains(str), "The api did not produce the op " + str);
        }
    }


    void assertOps(Api api, String... opMethodAndPathString){
        Set opStrings = new HashSet<>();
        for(Op op : api.buildOps()){
            opStrings.add(op.getMethod() + " " + op.getPath());
        }
        assertEquals(opMethodAndPathString.length, opStrings.size(), "The api produced the wrong number of operations");
        for(String str : opMethodAndPathString){
            assertTrue(opStrings.contains(str), "The api did not produce the op " + str);
        }
    }


    @Test
    public void test_buildOps_endpointDeclaresFullPath_withConflictingActions(){
        Api api = new Api();
        Endpoint ep1 = new Endpoint().withIncludeOn("GET,catalog/[{collection}]/[{resource}]/[{relationship}]");
        MockAction booksA = new MockAction().withIncludeOn("books/*");
        MockAction authorsA = new MockAction().withIncludeOn("authors/*");
        ep1.withActions(booksA, authorsA);
        api.withEndpoint(ep1);

        List<Op> ops = api.buildOps();

        System.out.println("----");
        for(Op op : ops)
            System.out.println(op.getMethod() + " " + op.getPath());
        System.out.println("----");

        assertEquals(6, ops.size());
        assertOp(ops, "GET", "catalog/books", 1, "collection", 1);
        assertOp(ops, "GET", "catalog/books/{resource}", 2, "resource", 2);
        assertOp(ops, "GET", "catalog/books/{resource}/{relationship}", 3, "resource", 2, "relationship", 3);

        assertEquals(6, ops.size());
        assertOp(ops, "GET", "catalog/authors", 1, "collection", 1);
        assertOp(ops, "GET", "catalog/authors/{resource}", 2, "resource", 2);
        assertOp(ops, "GET", "catalog/authors/{resource}/{relationship}", 3, "resource", 2, "relationship", 3);
    }


    @Test
    public void test_buildOps_endpointDeclaresFullPath_withOptionalConflictingActions(){
        Api api = new Api();
        Endpoint ep1 = new Endpoint().withIncludeOn("GET,catalog/[{collection}]/[{resource}]/[{relationship}]");
        MockAction booksA = new MockAction().withIncludeOn("books/*");
        MockAction authorsA = new MockAction().withIncludeOn("[authors]/*");
        ep1.withActions(booksA, authorsA);
        api.withEndpoint(ep1);

        List<Op> ops = api.buildOps();

        System.out.println("----");
        for(Op op : ops)
            System.out.println(op.getMethod() + " " + op.getPath());
        System.out.println("----");

        assertEquals(7, ops.size());
        assertOp(ops, "GET", "catalog", 0);
        assertOp(ops, "GET", "catalog/books", 1, "collection", 1);
        assertOp(ops, "GET", "catalog/books/{resource}", 2, "resource", 2);
        assertOp(ops, "GET", "catalog/books/{resource}/{relationship}", 3, "resource", 2, "relationship", 3);
        assertOp(ops, "GET", "catalog/authors", 1, "collection", 1);
        assertOp(ops, "GET", "catalog/authors/{resource}", 2, "resource", 2);
        assertOp(ops, "GET", "catalog/authors/{resource}/{relationship}", 3, "resource", 2, "relationship", 3);
    }


    @Test
    public void test_buildOps_endpointDeclaresFullPath_withOptionalVarConflictingActions(){
        Api api = new Api();
        Endpoint ep1 = new Endpoint().withIncludeOn("GET,catalog/[{collection}]/[{resource}]/[{relationship}]");
        ep1.withAction(new MockAction().withIncludeOn("books/*"));
        ep1.withAction(new MockAction().withIncludeOn("{var|authors}/*"));

        api.withEndpoint(ep1);

        List<Op> ops = api.buildOps();

        System.out.println("----");
        for(Op op : ops)
            System.out.println(op.getMethod() + " " + op.getPath());
        System.out.println("----");

        assertEquals(3, ops.size());
        assertOp(ops, "GET", "catalog/books", 2, "collection", 1);
        assertOp(ops, "GET", "catalog/books/{resource}", 3, "resource", 2);
        assertOp(ops, "GET", "catalog/books/{resource}/{relationship}", 4, "resource", 2, "relationship", 3);
    }


    @Test
    public void test_buildOps_endpointDeclaresFullPath(){
        Api api = new Api();
        Endpoint ep1 = new Endpoint().withIncludeOn("GET,catalog/[{collection}]/[{resource}]/[{relationship}]");
        MockAction booksA = new MockAction().withIncludeOn("books/*");
        ep1.withAction(booksA);
        api.withEndpoint(ep1);
        List<Op> ops = api.buildOps();

        System.out.println("----");
        for(Op op : ops)
            System.out.println(op.getMethod() + " " + op.getPath());
        System.out.println("----");

        assertEquals(3, ops.size());
        assertOp(ops, "GET", "catalog/books", 1, "collection", 1);
        assertOp(ops, "GET", "catalog/books/{resource}", 2, "resource", 2);
        assertOp(ops, "GET", "catalog/books/{resource}/{relationship}", 3, "resource", 2, "relationship", 3);
    }

    @Test
    public void test_buildOps_EndpointActionDeclaresFullPath(){
        Api api = new Api();
        Endpoint ep1 = new Endpoint().withIncludeOn("GET,catalog/*");
        MockAction booksA = new MockAction().withIncludeOn("[{collection}]/[{resource}]/[{relationship}]");
        ep1.withAction(booksA);
        api.withEndpoint(ep1);

        List<Op> ops = api.buildOps();
        System.out.println("----");
        for(Op op : ops)
            System.out.println(op.getMethod() + " " + op.getPath());
        System.out.println("----");

        assertEquals(4, ops.size());
        assertOp(ops, "GET", "catalog");
        assertOp(ops, "GET", "catalog/{collection}");
        assertOp(ops, "GET", "catalog/{collection}/{resource}");
        assertOp(ops, "GET", "catalog/{collection}/{resource}/{relationship}");
    }


    @Test
    public void test_buildOps_EndpointAndActionParamsGetApplied(){

        Api api = new Api().withIncludeOn("GET");
        api.withServer(new Server().withIncludeOn("GET"));
        Endpoint ep1 = new Endpoint().withIncludeOn("[{_collection}]/[{_resource}]/[{_relationship}]");
        MockAction dbA = new MockAction();
        dbA.withIncludeOn("books/[{bookId}]/[{author}]");
        dbA.withIncludeOn("author/[{authorId}]/[{books}]");
        ep1.withAction(dbA);
        api.withEndpoint(ep1);

        List<Op> ops = api.buildOps();

        System.out.println("----");
        for(Op op : ops)
            System.out.println(op.getMethod() + " " + op.getPath() + " - " + op.getParams());
        System.out.println("----");

        ArrayListValuedHashMap<String, Path> paths = api.buildRequestPaths();
        System.out.println(paths);

        assertOp(ops, "GET", "books", 1, "_collection", 0);
        assertOp(ops, "GET", "books/{bookId}", 3, "_collection", 0, "_resource", 1, "bookId", 1);
        assertOp(ops, "GET", "books/{bookId}/{author}", 5, "_collection", 0, "_resource", 1, "bookId", 1, "_relationship", 2, "author", 2 );

        assertOp(ops, "GET", "author", 1, "_collection", 0);
        assertOp(ops, "GET", "author/{authorId}", 3, "_collection", 0, "_resource", 1, "authorId", 1);
        assertOp(ops, "GET", "author/{authorId}/{books}", 5, "_collection", 0, "_resource", 1, "authorId", 1, "_relationship", 2, "books", 2 );
    }

    @Test
    public void expandAndFilterDuplicates(){
        List<Path> paths = new ArrayList();
        paths.add(new Path("{_collection}/[{_resource}]/[{_relationship}]"));
        paths.add(new Path("books/[{bookId}]/[{author}]"));
        paths.add(new Path("author/[{authorId}]/[{books}]"));

        paths = Path.expandOptionals(paths);
        paths = Path.filterDuplicates(paths);
        assertEquals("[{_collection}, {_collection}/{_resource}, {_collection}/{_resource}/{_relationship}, books, books/{bookId}, books/{bookId}/{author}, author, author/{authorId}, author/{authorId}/{books}]", paths.toString());

        List<Path> merged = Path.mergePaths(new ArrayList<>(), paths);
        assertEquals("[books, books/{_resource}, books/{_resource}/{_relationship}, author, author/{authorId}, author/{authorId}/{books}]", merged.toString());

        System.out.println(merged);
        System.out.println("-----");
    }



    @Test
    public void test_buildOps_ApiActionDeclaresFullPath(){
        Api api = new Api();

        Endpoint ep1 = new Endpoint().withIncludeOn("GET,catalog/[{collection}]/*");
        MockAction booksA = new MockAction().withIncludeOn("catalog/books/[{resource}]/[{relationship}]");
        api.withAction(booksA);
        api.withEndpoint(ep1);


        List<Op> ops = api.buildOps();

        System.out.println("----");
        for(Op op : ops)
            System.out.println(op.getMethod() + " " + op.getPath());
        System.out.println("----");

        assertEquals(3, ops.size());
        assertOp(ops, "GET", "catalog/books");
        assertOp(ops, "GET", "catalog/books/{resource}");
        assertOp(ops, "GET", "catalog/books/{resource}/{relationship}");
    }


//
//    @Test
//    public void test_buildOps_apiActionDeclaresFullPath(){
//        Engine engine = new Engine().withIncludeOn("servlet/*");
//        Api api = new Api().withIncludeOn("store/v1/*");
//        Endpoint ep1 = new Endpoint().withIncludeOn("catalog/[{collection}]/*");
//        MockAction booksA = new MockAction().withIncludeOn("catalog/books/[{resource}]/[{relationship}]");
//        api.withEndpoint(ep1);
//        api.withAction(booksA);
//
//        List<Path> paths = api.buildAllOperationPaths(engine);
//        assertEquals("[servlet/store/v1/catalog/books/[{resource}]/[{relationship}]]", paths.toString());
//    }
//
//    @Test
//    public void test_buildOps_MultipleEpActions(){
//        Engine engine = new Engine().withIncludeOn("servlet/*");
//        Api api = new Api().withIncludeOn("store/v1/*");
//        Endpoint ep1 = new Endpoint().withIncludeOn("catalog/[{collection}]/*");
//        MockAction booksA = new MockAction().withIncludeOn("/books/[{resource}]");
//        MockAction catsA = new MockAction().withIncludeOn("/categories/[catId]");
//        ep1.withActions(booksA, catsA);
//
//        MockAction carsA = new MockAction().withIncludeOn("catalog/cars/[carId]");
//        api.withEndpoint(ep1);
//        api.withAction(booksA);
//
//        List<Path> paths = api.buildAllOperationPaths(engine);
//        assertEquals("[servlet/store/v1/catalog/books/[{resource}], servlet/store/v1/catalog/categories/[catId]]", paths.toString());
//    }
//
//    @Test
//    public void test_buildOps_MultipleEpActions2(){
//        Engine engine = new Engine().withIncludeOn("servlet/*");
//        Api api = new Api().withIncludeOn("store/v1/*");
//        Endpoint ep1 = new Endpoint().withIncludeOn("catalog/[{collection}]/*");
//        MockAction booksA = new MockAction().withIncludeOn("/books/[{resource}]");
//        MockAction booksB = new MockAction().withIncludeOn("/books/[{bookId}]/[relationship]");
//        ep1.withActions(booksA, booksB);
//
//        api.withEndpoint(ep1);
//        api.withAction(booksA);
//
//        List<Path> paths = api.buildAllOperationPaths(engine);
//        assertEquals("[servlet/store/v1/catalog/books/[{resource}], servlet/store/v1/catalog/categories/[catId]]", paths.toString());
//    }



//    @Test
//    public void buildOperations_error_on_conflicting_path_params(){
//        Api api = new Api().withIncludeOn("test/{var1}/*").withEndpoint("ep1/{var1}/*", new Action()).withCollection(new Collection().withName("test"));
//
//        try{
//            api.getOperations();
//            fail("Should have thrown an error because variable var1 is mismatched");
//        }
//        catch(Exception ex){
//            System.out.println(ex.getMessage());
//        }
//    }
//
//    @Test
//    public void buildOperations_duplicateOperationIdsGetResolved(){
//        Api api = new Api().withIncludeOn("test/{var1}/*").withEndpoint("ep1/{var1}/*", new Action()).withCollection(new Collection().withName("test"));
//
//        try{
//            api.getOperations();
//            fail("Should have thrown an error because variable var1 is mismatched");
//        }
//        catch(Exception ex){
//            System.out.println(ex.getMessage());
//        }
//    }



}
