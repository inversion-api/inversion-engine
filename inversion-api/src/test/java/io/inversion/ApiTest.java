package io.inversion;

import io.inversion.action.db.DbAction;
import io.inversion.action.misc.MockAction;
import io.inversion.utils.Path;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ApiTest {

    @Test
    public void test_generatePaths_endpoint_optional_literal_overrides_action_var() {
        DbAction dbAction = new DbAction();
        Db       bookDb   = new BooksDb();
        Api api = new Api()//
                .withDb(bookDb)
                .withEndpoint("GET,[books]/[{bookKey}]/*", dbAction)
                .withEndpoint("GET,[authors]/*", dbAction);
        ;

        bookDb.startup(api);
        String actual = api.generatePaths().toString();
        String expected = "{GET={Endpoint - includes: [[GET,[authors]/*]]=[[authors], [authors/{authorId}], [authors/{authorId}/books]], Endpoint - includes: [[GET,[books]/[{bookKey}]/*]]=[[books], [books/{bookKey}], [books/{bookKey}/author]]}}";
        System.out.println(actual);
        assertEquals(expected, actual);
    }


    @Test
    public void test_generatePaths_endpoint_identifies_full_path() {
        Api api = new Api()//
                .withEndpoint("GET,openapi.json,openapi.yaml", new MockAction())
                .withEndpoint("GET,rapidoc.html", new MockAction());

        String expected = "{GET={Endpoint - includes: [[GET,openapi.json,openapi.yaml]]=[[openapi.json], [openapi.yaml]], Endpoint - includes: [[GET,rapidoc.html]]=[[rapidoc.html]]}}";
        String actual = api.generatePaths().toString();
        System.out.println(actual);
        assertEquals(expected, actual);
    }

    @Test
    public void test_generatePaths_no_endpoint_vars_no_endpoint_optionals() {
        Api        api    = new Api();
        Endpoint   ep1    = new Endpoint().withIncludeOn("GET,catalog/*");
        MockAction booksA = new MockAction().withIncludeOn("[{collection}]/[{resource}]/[{relationship}]");

        ep1.withAction(booksA);
        api.withEndpoint(ep1);

        String expected = "{GET={Endpoint - includes: [[GET,catalog/*]]=[[catalog], [catalog/{collection}], [catalog/{collection}/{resource}], [catalog/{collection}/{resource}/{relationship}]]}}";
        String actual   = api.generatePaths().toString();
        assertEquals(expected, actual);
    }

    @Test
    public void test_generatePaths_no_endpoint_vars_has_endpoint_optionals() {
        Api api = new Api().withEndpoint(
                new Endpoint().withIncludeOn("GET,catalog/[books]/*")
                        .withAction(new MockAction().withIncludeOn("[{collection}]/[{resource}]/[{relationship}]")));

        String expected = "{GET={Endpoint - includes: [[GET,catalog/[books]/*]]=[[catalog], [catalog/books], [catalog/books/{resource}], [catalog/books/{resource}/{relationship}]]}}";
        String actual   = api.generatePaths().toString();
        assertEquals(expected, actual);
    }

    @Test
    public void test_generatePaths_action_literals_override_endpoint_vars() {
        Api api = new Api();
        api.withAction(new MockAction().withIncludeOn("a/b/{named1}/{named2}/*"));
        api.withAction(new MockAction().withIncludeOn("a/b/{named3}/{named4}/*"));
        api.withEndpoint("GET,{endpointNamed1}/b/{endpointNamed2}/[{Ecoll}]/[{Eent:e}]/[{Eid}]/*");

        String expected = "{GET={Endpoint - includes: [[GET,{endpointNamed1}/b/{endpointNamed2}/[{Ecoll}]/[{Eent:e}]/[{Eid}]/*]]=[[a/b/{endpointNamed2}/{Ecoll}], [a/b/{endpointNamed2}/{Ecoll}/{Eent:e}], [a/b/{endpointNamed2}/{Ecoll}/{Eent:e}/{Eid}]]}}";
        String actual   = api.generatePaths().toString();
        System.out.println(actual);
        assertEquals(expected, actual);
    }

    @Test
    public void test_generatePaths_api_action_declares_full_path() {
        Api        api    = new Api();
        Endpoint   ep1    = new Endpoint().withIncludeOn("GET,catalog/[{collection}]/*");
        MockAction booksA = new MockAction().withIncludeOn("catalog/books/[{resource}]/[{relationship}]");
        api.withAction(booksA);
        api.withEndpoint(ep1);

        String actual = api.generatePaths().toString();
        String expected = "{GET={Endpoint - includes: [[GET,catalog/[{collection}]/*]]=[[catalog/books], [catalog/books/{resource}], [catalog/books/{resource}/{relationship}]]}}";
        System.out.println(actual);
        assertEquals(expected, actual);
    }


    @Test
    public void test_generatePaths_no_concrete_path() {
        Api api = new Api("test").withServer(new Server().withIncludeOn("test/*"))
                .withAction(new MockAction("mock1"))//
                .withEndpoint(new Endpoint("[{collection:collectionA}]/*").withName("epA"))
                .withEndpoint(new Endpoint("[{collection:collection1}]/*").withName("ep1"))
                .withEndpoint(new Endpoint("[{collection:collection2}]/*").withName("ep2"));
                ;

        String actual = api.generatePaths().toString();
        String expected = "{GET={Endpoint:ep1 - includes: [[[{collection:collection1}]/*]]=[[{collection:collection1}]], Endpoint:ep2 - includes: [[[{collection:collection2}]/*]]=[[{collection:collection2}]], Endpoint:epA - includes: [[[{collection:collectionA}]/*]]=[[{collection:collectionA}]]}, POST={Endpoint:ep1 - includes: [[[{collection:collection1}]/*]]=[[{collection:collection1}]], Endpoint:ep2 - includes: [[[{collection:collection2}]/*]]=[[{collection:collection2}]], Endpoint:epA - includes: [[[{collection:collectionA}]/*]]=[[{collection:collectionA}]]}, PUT={Endpoint:ep1 - includes: [[[{collection:collection1}]/*]]=[[{collection:collection1}]], Endpoint:ep2 - includes: [[[{collection:collection2}]/*]]=[[{collection:collection2}]], Endpoint:epA - includes: [[[{collection:collectionA}]/*]]=[[{collection:collectionA}]]}, PATCH={Endpoint:ep1 - includes: [[[{collection:collection1}]/*]]=[[{collection:collection1}]], Endpoint:ep2 - includes: [[[{collection:collection2}]/*]]=[[{collection:collection2}]], Endpoint:epA - includes: [[[{collection:collectionA}]/*]]=[[{collection:collectionA}]]}, DELETE={Endpoint:ep1 - includes: [[[{collection:collection1}]/*]]=[[{collection:collection1}]], Endpoint:ep2 - includes: [[[{collection:collection2}]/*]]=[[{collection:collection2}]], Endpoint:epA - includes: [[[{collection:collectionA}]/*]]=[[{collection:collectionA}]]}}";
        System.out.println(actual);
        assertEquals(expected, actual);
    }

//    @Test
//    public void test_generatePaths_action_identifies_full_path_with_simple_regex() {
//        Api api = new Api()//
//                .withEndpoint("GET", new MockAction().withIncludeOn("{file:openapi\\.json}"))
//                .withEndpoint("GET,rapidoc.html", new MockAction());
//        assertPaths(api, "GET openapi.json", "GET openapi.yaml", "GET rapidoc.html");
//    }

//    @Test
//    public void test_generatePaths_solveTrivialRegexes() {
//        Api api = new Api()//
//                .withEndpoint("GET,store/{var1:home}/[{_collection:books|toys}]", new MockAction());
//
//        assertPaths(api, "GET openapi.json", "GET openapi.yaml", "GET rapidoc.html");
//    }


    @Test
    public void test_buildOps_endpoint_optional_literal_overrides_action_var() {
        DbAction dbAction = new DbAction();
        Db       bookDb   = new BooksDb();
        Api api = new Api()//
                .withDb(bookDb)
                .withEndpoint("GET,[books]/[{bookKey}]/*", dbAction)
                .withEndpoint("GET,[authors]/*", dbAction);

        bookDb.startup(api);

        String actual = api.configureOps().toString();
        System.out.println(actual);
        String expected = "[{name=findAuthors, method=GET, path=authors, collection=authors, relationship=null, actions=[DbAction], params=[[PATH: {_collection},0], [QUERY: {pageNumber},0], [QUERY: {pageSize},0], [QUERY: {offset},0], [QUERY: {sort},0], [QUERY: {q},0]]}, {name=getAuthorsByAuthorId, method=GET, path=authors/{authorId}, collection=authors, relationship=null, actions=[DbAction], params=[[PATH: {authorId},1], [PATH: {_collection},0], [PATH: {_resource},1]]}, {name=findAuthorsRelatedBooksByAuthorId, method=GET, path=authors/{authorId}/books, collection=authors, relationship=books, actions=[DbAction], params=[[PATH: {authorId},1], [PATH: {_collection},0], [PATH: {_resource},1], [PATH: {_relationship},2]]}, {name=findBooks, method=GET, path=books, collection=books, relationship=null, actions=[DbAction], params=[[PATH: {_collection},0], [QUERY: {pageNumber},0], [QUERY: {pageSize},0], [QUERY: {offset},0], [QUERY: {sort},0], [QUERY: {q},0]]}, {name=getBooksByBookKey, method=GET, path=books/{bookKey}, collection=books, relationship=null, actions=[DbAction], params=[[PATH: {bookKey},1], [PATH: {_collection},0], [PATH: {_resource},1]]}, {name=findBooksRelatedAuthorByBookKey, method=GET, path=books/{bookKey}/author, collection=books, relationship=author, actions=[DbAction], params=[[PATH: {bookKey},1], [PATH: {_collection},0], [PATH: {_resource},1], [PATH: {_relationship},2]]}]";
        assertEquals(expected, actual);
    }




    @Test
    public void test_buildOps_endpointDeclaresFullPath_withOptionalVarConflictingActions() {
        Api api = new Api().withEndpoint(
                new Endpoint()
                        .withIncludeOn("GET,catalog/[{collection}]/[{resource}]/[{relationship}]")
                        .withAction(new MockAction("a1").withIncludeOn("books/*"))
                        .withAction(new MockAction("a2").withIncludeOn("{var|authors}/*"))
        );

        String actual = api.configureOps().toString();
        System.out.println(actual);
        String expected = "[{name=getBooks, method=GET, path=catalog/books, collection=null, relationship=null, actions=[a1, a2], params=[[PATH: {var|authors},1]]}, {name=getResourceByResource, method=GET, path=catalog/books/{resource}, collection=null, relationship=null, actions=[a1, a2], params=[[PATH: {resource},2], [PATH: {var|authors},1]]}, {name=getRelationshipByResourceByRelationship, method=GET, path=catalog/books/{resource}/{relationship}, collection=null, relationship=null, actions=[a1, a2], params=[[PATH: {resource},2], [PATH: {relationship},3], [PATH: {var|authors},1]]}, {name=getCollectionByCollection, method=GET, path=catalog/{collection}, collection=null, relationship=null, actions=[a1, a2], params=[[PATH: {collection},1], [PATH: {var|authors},1]]}, {name=getResourceByCollectionByResource, method=GET, path=catalog/{collection}/{resource}, collection=null, relationship=null, actions=[a1, a2], params=[[PATH: {collection},1], [PATH: {resource},2], [PATH: {var|authors},1]]}, {name=getRelationshipByCollectionByResourceByRelationship, method=GET, path=catalog/{collection}/{resource}/{relationship}, collection=null, relationship=null, actions=[a1, a2], params=[[PATH: {collection},1], [PATH: {resource},2], [PATH: {relationship},3], [PATH: {var|authors},1]]}]";
        assertEquals(expected, actual);
    }


    @Test
    public void test_buildOps_endpointDeclaresFullPath() {
        Api        api    = new Api();
        Endpoint   ep1    = new Endpoint().withIncludeOn("GET,catalog/[{collection}]/[{resource}]/[{relationship}]");
        MockAction booksA = new MockAction().withIncludeOn("books/*");
        ep1.withAction(booksA);
        api.withEndpoint(ep1);

        String actual = api.configureOps().toString();
        System.out.println(actual);
        String expected = "[{name=getBooks, method=GET, path=catalog/books, collection=null, relationship=null, actions=[MockAction], params=[]}, {name=getResourceByResource, method=GET, path=catalog/books/{resource}, collection=null, relationship=null, actions=[MockAction], params=[[PATH: {resource},2]]}, {name=getRelationshipByResourceByRelationship, method=GET, path=catalog/books/{resource}/{relationship}, collection=null, relationship=null, actions=[MockAction], params=[[PATH: {resource},2], [PATH: {relationship},3]]}]";
        assertEquals(expected, actual);
    }


    @Test
    public void test_buildOps_ApiActionDeclaresFullPath() {
        Api        api    = new Api();
        Endpoint   ep1    = new Endpoint().withIncludeOn("GET,catalog/[{collection}]/*");
        MockAction booksA = new MockAction().withIncludeOn("catalog/books/[{resource}]/[{relationship}]");
        api.withAction(booksA);
        api.withEndpoint(ep1);

        String actual = api.configureOps().toString();
        System.out.println(actual);
        String expected = "[{name=getBooks, method=GET, path=catalog/books, collection=null, relationship=null, actions=[MockAction], params=[]}, " +
                "{name=getResourceByResource, method=GET, path=catalog/books/{resource}, collection=null, relationship=null, actions=[MockAction], params=[[PATH: {resource},2]]}, " +
                "{name=getRelationshipByResourceByRelationship, method=GET, path=catalog/books/{resource}/{relationship}, collection=null, relationship=null, actions=[MockAction], params=[[PATH: {resource},2], [PATH: {relationship},3]]}]";
        assertEquals(expected, actual);
    }



//    @Test
//    public void test_buildOps_actions_and_collections_no_db() {
//        Api api = new Api()
//                .withEndpoint("GET,[{_collection:books}]", new MockAction())
//                .withEndpoint("GET,[{_collection:toys}]", new MockAction());
//
//        BooksDb.makeTestCollections().forEach(c -> api.withCollection(c));
//
//        String actual = api.configureOps().toString();
//        System.out.println(actual);
//        String expected = "";
//        assertEquals(expected, actual);
//    }

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
