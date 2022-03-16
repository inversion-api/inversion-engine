package io.inversion;

import io.inversion.action.db.DbAction;
import io.inversion.action.misc.MockAction;
import io.inversion.utils.Path;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ApiTest {


    void assertPaths(Api api, String... opMethodAndPathString) {
        ArrayListValuedHashMap<String, Path> paths = api.buildRequestPaths();

        Set opStrings = new HashSet<>();
        for (String method : paths.keySet()) {
            for (Path path : paths.get(method)) {
                opStrings.add(method + " " + path);
            }
        }
        assertEquals(opMethodAndPathString.length, opStrings.size(), "The api produced the wrong number of operations");
        for (String str : opMethodAndPathString) {
            assertTrue(opStrings.contains(str), "The api did not produce the op " + str);
        }
    }

    void assertParameter(Op op, String key, int index) {
        for (Param p : op.getParams()) {
            if (key.equalsIgnoreCase(p.getKey()) && p.getIndex() == index)
                return;
        }
        fail("Parameter '" + key + "' at index " + index + " not found in operation");
    }

//    void assertOp(List<Op> ops, String method, String displayPath) {
//        assertOp(ops, method, displayPath, -1);
//    }

//    void assertOp(List<Op> ops, String method, String displayPath, int numParams, Object... paramKeyIndexPairs) {
//        for (Op op : ops) {
//            if (op.getMethod().equalsIgnoreCase(method) && op.getPath().toString().equalsIgnoreCase(displayPath)) {
//                if (numParams > -1)
//                    assertEquals(numParams, op.getParams().size());
//                for (int i = 0; paramKeyIndexPairs != null && i < paramKeyIndexPairs.length - 1; i += 2) {
//                    assertParameter(op, (String) paramKeyIndexPairs[i], (Integer) paramKeyIndexPairs[i + 1]);
//                }
//                return;
//            }
//        }
//        fail("Operation " + method + " " + displayPath + " not found");
//    }


    void assertOps(Api api, String... opToStrings) {
        Set opStrings = new HashSet<>();
        for (Op op : api.buildOps()) {
            opStrings.add(op.toString());
        }
        assertEquals(opToStrings.length, opStrings.size(), "The api produced the wrong number of operations");
        for (String str : opToStrings) {
            assertTrue(opStrings.contains(str), "The api did not produce the op " + str);
        }
    }


    @Test
    public void test_buildRequestPaths_endpoint_optional_literal_overrides_action_var() {
        DbAction dbAction = new DbAction();
        Db       bookDb   = new BooksDb();
        Api api = new Api()//
                .withDb(bookDb)
                .withEndpoint("GET,[books]/[{bookKey}]/*", dbAction)
                .withEndpoint("GET,[authors]/*", dbAction);

        assertPaths(api, "GET books"
                , "GET books/{bookKey}"
                , "GET books/{bookKey}/{_relationship}"
                , "GET authors"
                , "GET authors/{_resource}"
                , "GET authors/{_resource}/{_relationship}"
        );

        bookDb.startup(api);

        assertOps(api,
                "{name=findAuthors, method=GET, path=authors, collection=authors, relationship=null, actions=[DbAction], params=[[PATH: {_collection},0], [QUERY: {page},0], [QUERY: {size},0], [QUERY: {sort},0], [QUERY: {q},0]]}"
                , "{name=getAuthorsByAuthorId, method=GET, path=authors/{authorId}, collection=authors, relationship=null, actions=[DbAction], params=[[PATH: {_collection},0], [PATH: {_resource},1]]}"
                , "{name=findRelatedBooksByAuthorId, method=GET, path=authors/{authorId}/books, collection=authors, relationship=books, actions=[DbAction], params=[[PATH: {_collection},0], [PATH: {_resource},1], [PATH: {_relationship},2], [QUERY: {page},0], [QUERY: {size},0], [QUERY: {sort},0], [QUERY: {q},0]]}"
                , "{name=findBooks, method=GET, path=books, collection=books, relationship=null, actions=[DbAction], params=[[PATH: {_collection},0], [QUERY: {page},0], [QUERY: {size},0], [QUERY: {sort},0], [QUERY: {q},0]]}"
                , "{name=getBooksByBookKey, method=GET, path=books/{bookKey}, collection=books, relationship=null, actions=[DbAction], params=[[PATH: {bookKey},1], [PATH: {_collection},0], [PATH: {_resource},1]]}"
                , "{name=findRelatedAuthorByBookKey, method=GET, path=books/{bookKey}/author, collection=books, relationship=author, actions=[DbAction], params=[[PATH: {bookKey},1], [PATH: {_collection},0], [PATH: {_resource},1], [PATH: {_relationship},2], [QUERY: {page},0], [QUERY: {size},0], [QUERY: {sort},0], [QUERY: {q},0]]}"
        );
    }

    @Test
    public void test_buildRequestPaths_endpoint_identifies_full_path() {
        Api api = new Api()//
                .withEndpoint("GET,openapi.json,openapi.yaml", new MockAction())
                .withEndpoint("GET,rapidoc.html", new MockAction());
        assertPaths(api, "GET openapi.json", "GET openapi.yaml", "GET rapidoc.html");
    }

    @Test
    public void test_buildRequestPaths_action_identifies_full_path() {
        Api api = new Api()//
                .withEndpoint("GET", new MockAction().withIncludeOn("openapi.json,openapi.yaml"))
                .withEndpoint("GET, rapidoc.html", new MockAction());
        assertPaths(api, "GET openapi.json", "GET openapi.yaml", "GET rapidoc.html");
    }

    @Test
    public void test_buildRequestPaths_no_endpoint_vars_no_endpoint_optionals() {
        Api        api    = new Api();
        Endpoint   ep1    = new Endpoint().withIncludeOn("GET,catalog/*");
        MockAction booksA = new MockAction().withIncludeOn("[{collection}]/[{resource}]/[{relationship}]");

        ep1.withAction(booksA);
        api.withEndpoint(ep1);

        String expected = "{GET=[catalog, catalog/{collection}, catalog/{collection}/{resource}, catalog/{collection}/{resource}/{relationship}]}";
        String actual   = api.buildRequestPaths().toString();
        assertEquals(expected, actual);
    }

    @Test
    public void test_buildRequestPaths_no_endpoint_vars_has_endpoint_optionals() {
        Api api = new Api().withEndpoint(
                new Endpoint().withIncludeOn("GET,catalog/[books]/*")
                        .withAction(new MockAction().withIncludeOn("[{collection}]/[{resource}]/[{relationship}]")));

        String expected = "{GET=[catalog, catalog/books, catalog/books/{resource}, catalog/books/{resource}/{relationship}]}";
        String actual   = api.buildRequestPaths().toString();
        assertEquals(expected, actual);
    }

    @Test
    public void test_buildRequestPaths_endpoint_vars_override_action_literal() {
        Api api = new Api();
        api.withAction(new MockAction().withIncludeOn("a/b/{named1}/{named2}/*"));
        api.withAction(new MockAction().withIncludeOn("a/b/{named3}/{named4}/*"));
        api.withEndpoint("GET,{endpointNamed1}/b/{endpointNamed2}/[{Ecoll}]/[{Eent:e}]/[{Eid}]/*");

        api.buildRequestPaths();
    }


//    @Test
//    public void test_buildRequestPaths_action_identifies_full_path_with_simple_regex() {
//        Api api = new Api()//
//                .withEndpoint("GET", new MockAction().withIncludeOn("{file:openapi\\.json}"))
//                .withEndpoint("GET,rapidoc.html", new MockAction());
//        assertPaths(api, "GET openapi.json", "GET openapi.yaml", "GET rapidoc.html");
//    }

//    @Test
//    public void test_buildRequestPaths_solveTrivialRegexes() {
//        Api api = new Api()//
//                .withEndpoint("GET,store/{var1:home}/[{_collection:books|toys}]", new MockAction());
//
//        assertPaths(api, "GET openapi.json", "GET openapi.yaml", "GET rapidoc.html");
//    }


    @Test
    public void test_buildOps_actions_and_collections_no_db() {
        Api api = new Api()
                .withEndpoint("GET,[{_collection:books}]", new MockAction())
                .withEndpoint("GET,[{_collection:toys}]", new MockAction());

        BooksDb.makeTestCollections().forEach(c -> api.withCollection(c));
        assertOps(api
                , "{name=findBooks, method=GET, path=books, collection=books, relationship=null, actions=[MockAction], params=[[PATH: {_collection},0, [books]]]}"
                , "{name=findToys, method=GET, path=toys, collection=null, relationship=null, actions=[MockAction], params=[[PATH: {_collection},0, [toys]]]}"
        );
    }


    @Test
    public void test_buildOps_endpointDeclaresFullPath_withOptionalVarConflictingActions() {
        Api api = new Api().withEndpoint(
                new Endpoint()
                        .withIncludeOn("GET,catalog/[{collection}]/[{resource}]/[{relationship}]")
                        .withAction(new MockAction("a1").withIncludeOn("books/*"))
                        .withAction(new MockAction("a2").withIncludeOn("{var|authors}/*"))
        );

        api.buildOps();
        assertOps(api
                , "{name=getCollectionByCollection, method=GET, path=catalog/{collection}, collection=null, relationship=null, actions=[a1, a2], params=[[PATH: {collection},1], [PATH: {var|authors},1]]}"
                , "{name=getResourceByCollectionByResource, method=GET, path=catalog/{collection}/{resource}, collection=null, relationship=null, actions=[a1, a2], params=[[PATH: {collection},1], [PATH: {resource},2], [PATH: {var|authors},1]]}"
                , "{name=getRelationshipByCollectionByResourceByRelationship, method=GET, path=catalog/{collection}/{resource}/{relationship}, collection=null, relationship=null, actions=[a1, a2], params=[[PATH: {collection},1], [PATH: {resource},2], [PATH: {relationship},3], [PATH: {var|authors},1]]}"
        );
    }


    @Test
    public void test_buildOps_endpointDeclaresFullPath() {
        Api        api    = new Api();
        Endpoint   ep1    = new Endpoint().withIncludeOn("GET,catalog/[{collection}]/[{resource}]/[{relationship}]");
        MockAction booksA = new MockAction().withIncludeOn("books/*");
        ep1.withAction(booksA);
        api.withEndpoint(ep1);

        assertOps(api
                , "{name=getCollectionByCollection, method=GET, path=catalog/{collection}, collection=null, relationship=null, actions=[MockAction], params=[[PATH: {collection},1]]}"
                , "{name=getResourceByCollectionByResource, method=GET, path=catalog/{collection}/{resource}, collection=null, relationship=null, actions=[MockAction], params=[[PATH: {collection},1], [PATH: {resource},2]]}"
                , "{name=getRelationshipByCollectionByResourceByRelationship, method=GET, path=catalog/{collection}/{resource}/{relationship}, collection=null, relationship=null, actions=[MockAction], params=[[PATH: {collection},1], [PATH: {resource},2], [PATH: {relationship},3]]}"
        );
    }


    @Test
    public void test_buildOps_ApiActionDeclaresFullPath() {
        Api        api    = new Api();
        Endpoint   ep1    = new Endpoint().withIncludeOn("GET,catalog/[{collection}]/*");
        MockAction booksA = new MockAction().withIncludeOn("catalog/books/[{resource}]/[{relationship}]");
        api.withAction(booksA);
        api.withEndpoint(ep1);

        assertOps(api,
                "{name=getCollectionByCollection, method=GET, path=catalog/{collection}, collection=null, relationship=null, actions=[MockAction], params=[[PATH: {collection},1]]}"
                , "{name=getResourceByCollectionByResource, method=GET, path=catalog/{collection}/{resource}, collection=null, relationship=null, actions=[MockAction], params=[[PATH: {resource},2], [PATH: {collection},1]]}"
                , "{name=getRelationshipByCollectionByResourceByRelationship, method=GET, path=catalog/{collection}/{resource}/{relationship}, collection=null, relationship=null, actions=[MockAction], params=[[PATH: {resource},2], [PATH: {relationship},3], [PATH: {collection},1]]}");
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
