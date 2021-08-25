package io.inversion;

import com.github.curiousoddman.rgxgen.RgxGen;
import com.github.curiousoddman.rgxgen.iterators.StringIterator;
import io.inversion.action.misc.MockAction;
import io.inversion.utils.Path;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ApiTest {

    void join(String expected, String leftPath, String rightPath){
        Path path =  Path.joinPaths(new Path(leftPath), new Path(rightPath));
        assertEquals(expected, path != null ? path.toString() : null);
    }

    @Test
    public void joinPath(){
        join("servlet/path/api/v1/*", "/servlet/path/*", "api/v1/*");
        join("servlet/path/api/v1/[books]/*", "/servlet/path/*", "api/v1/[books]/*");
        join("api/v1/[{collection}]/[{resource}]/[{relationship}]", "/api/v1/[{collection}]/[{resource}]/[{relationship}]", "/*");
        join("api/v1/*", "/*", "api/v1/*");
        join(null, "servlet/[api1/*", "api2/*");
    }


    void assertParameter(Operation op, String key, int index){
        for(Parameter p : op.getParameters()){
            if(key.equalsIgnoreCase(p.getKey()) && p.getIndex() == index)
                return;
        }
        fail("Parameter '" + key + "' at index " + index + " not found in operation");
    }

    void assertOp(List<Operation> ops, String method, String displayPath){
        assertOp(ops, method, displayPath, -1);
    }

    void assertOp(List<Operation> ops, String method, String displayPath, int numParams, Object... paramKeyIndexPairs){
        for(Operation op : ops){
            if(op.getMethod().equalsIgnoreCase(method) && op.getDisplayPath().toString().equalsIgnoreCase(displayPath)){
                if(numParams > -1)
                    assertEquals(numParams, op.getParameters().size());
                for(int i=0; paramKeyIndexPairs != null && i<paramKeyIndexPairs.length -1; i+=2){
                    assertParameter(op, (String)paramKeyIndexPairs[i], (Integer)paramKeyIndexPairs[i+1]);
                }
                return;
            }
        }
        fail("Operation " + method + " " + displayPath + " not found");
    }


    @Test
    public void buildAllOperationPaths_endpointDeclaresFullPath(){
        Engine engine = new Engine().withIncludeOn("GET", "servlet/[{apiName}]/*");
        Api api = new Api().withIncludeOn("GET", "store/v1/[{something}]/*");
        Endpoint ep1 = new Endpoint().withIncludeOn("GET", "catalog/[{collection}]/[{resource}]/[{relationship}]");
        MockAction booksA = new MockAction().withIncludeOn("GET", "books/*");
        ep1.withAction(booksA);
        api.withEndpoint(ep1);
        engine.withApi(api);

        List<Operation> ops = engine.buildOperations();

        System.out.println("----");
        for(Operation op : ops)
            System.out.println(op.getMethod() + " " + op.getDisplayPath());
        System.out.println("----");

        assertEquals(3, ops.size());
        assertOp(ops, "GET", "servlet/store/v1/catalog/books", 3, "apiName", 1, "something", 3, "collection", 4);
        assertOp(ops, "GET", "servlet/store/v1/catalog/books/{resource}", 4, "resource", 5);
        assertOp(ops, "GET", "servlet/store/v1/catalog/books/{resource}/{relationship}", 5, "resource", 5, "relationship", 6);
    }

    @Test
    public void buildAllOperationPaths_EndpointActionDeclaresFullPath(){
        Engine engine = new Engine().withIncludeOn("GET", "servlet/[{apiName}]/*");
        Api api = new Api().withIncludeOn("GET", "store/v1/[{something}]/*");
        Endpoint ep1 = new Endpoint().withIncludeOn("GET", "catalog/*");
        MockAction booksA = new MockAction().withIncludeOn("GET", "[{collection}]/[{resource}]/[{relationship}]");
        ep1.withAction(booksA);
        api.withEndpoint(ep1);
        engine.withApi(api);

        List<Operation> ops = engine.buildOperations();

        System.out.println("----");
        for(Operation op : ops)
            System.out.println(op.getMethod() + " " + op.getDisplayPath());
        System.out.println("----");

        assertEquals(4, ops.size());
        assertOp(ops, "GET", "servlet/store/v1/catalog");
        assertOp(ops, "GET", "servlet/store/v1/catalog/{collection}");
        assertOp(ops, "GET", "servlet/store/v1/catalog/{collection}/{resource}");
        assertOp(ops, "GET", "servlet/store/v1/catalog/{collection}/{resource}/{relationship}");

    }


    @Test
    public void buildAllOperationPaths_EndpointAndActionParamsGetApplied(){
        Engine engine = new Engine().withIncludeOn("GET", "*");
        Api api = new Api().withIncludeOn("GET", "*");
        Endpoint ep1 = new Endpoint().withIncludeOn("GET", "[{_collection}]/[{_resource}]/[{_relationship}]");
        MockAction dbA = new MockAction();
        dbA.withIncludeOn("GET", "books/[{bookId}]/[{author}]");
        dbA.withIncludeOn("GET", "author/[{authorId}]/[{books}]");

        ep1.withAction(dbA);
        api.withEndpoint(ep1);
        engine.withApi(api);

        List<Operation> ops = engine.buildOperations();

        System.out.println("----");
        for(Operation op : ops)
            System.out.println(op.getMethod() + " " + op.getDisplayPath() + " - " + op.getParameters());
        System.out.println("----");



        ArrayListValuedHashMap<String, Path> paths = engine.enumeratePaths();
        System.out.println(paths);

        assertOp(ops, "GET", "books", 1, "_collection", 0);
        assertOp(ops, "GET", "books/{bookId}", 3, "_collection", 0, "_resource", 1, "bookId", 1);
        assertOp(ops, "GET", "books/{bookId}/{author}", 5, "_collection", 0, "_resource", 1, "bookId", 1, "_relationship", 2, "author", 2 );

        assertOp(ops, "GET", "author", 1, "_collection", 0);
        assertOp(ops, "GET", "author/{authorId}", 3, "_collection", 0, "_resource", 1, "authorId", 1);
        assertOp(ops, "GET", "author/{authorId}/{books}", 5, "_collection", 0, "_resource", 1, "authorId", 1, "_relationship", 2, "books", 2 );
    }

    @Test
    public void buildAllOperationPaths_EndpointActionDeclaresDisjointOperationPathsFullOverlappingWithVariables2(){
        Engine engine = new Engine().withIncludeOn("GET", "*");
        Api api = new Api().withIncludeOn("GET", "*");
        Endpoint ep1 = new Endpoint().withIncludeOn("GET", "*");
        MockAction dbA = new MockAction();
        dbA.withIncludeOn("GET", "{_collection}/[{_resource}]/[{_relationship}]");
        dbA.withIncludeOn("GET", "books/[{bookId}]/[{author}]");
        dbA.withIncludeOn("GET", "author/[{authorId}]/[{books}]");

        ep1.withAction(dbA);
        api.withEndpoint(ep1);
        engine.withApi(api);

        List<Operation> ops = engine.buildOperations();

        System.out.println("----");
        for(Operation op : ops)
            System.out.println(op.getMethod() + " " + op.getDisplayPath() + " - " + op.getParameters());
        System.out.println("----");



        ArrayListValuedHashMap<String, Path> paths = engine.enumeratePaths();
        System.out.println(paths);

        assertOp(ops, "GET", "books", 1, "_collection", 0);
        assertOp(ops, "GET", "books/{_resource}", 3, "_collection", 0, "_resource", 1, "bookId", 1);
        assertOp(ops, "GET", "books/{_resource}/{_relationship}", 5, "_collection", 0, "_resource", 1, "bookId", 1, "_relationship", 2, "author", 2 );

        assertOp(ops, "GET", "author", 1, "_collection", 0);
        assertOp(ops, "GET", "author/{_resource}", 3, "_collection", 0, "_resource", 1, "authorId", 1);
        assertOp(ops, "GET", "author/{_resource}/{_relationship}", 5, "_collection", 0, "_resource", 1, "authorId", 1, "_relationship", 2, "books", 2 );
    }


    @Test
    public void expandAndFilterDuplicates(){
        List<Path> paths = new ArrayList();
        paths.add(new Path("{_collection}/[{_resource}]/[{_relationship}]"));
        paths.add(new Path("books/[{bookId}]/[{author}]"));
        paths.add(new Path("author/[{authorId}]/[{books}]"));

        paths = Engine.expandOptionalsAndFilterDuplicates(paths);
        assertEquals("[{_collection}, {_collection}/{_resource}, {_collection}/{_resource}/{_relationship}, books, books/{bookId}, books/{bookId}/{author}, author, author/{authorId}, author/{authorId}/{books}]", paths.toString());

        List<Path> merged = Endpoint.mergePaths(new ArrayList<>(), paths);
        assertEquals("[books, books/{_resource}, books/{_resource}/{_relationship}, author, author/{authorId}, author/{authorId}/{books}]", merged.toString());

        System.out.println(merged);
        System.out.println("-----");
    }



    @Test
    public void buildAllOperationPaths_ApiActionDeclaresFullPath(){
        Engine engine = new Engine().withIncludeOn("GET", "servlet/*");
        Api api = new Api().withIncludeOn("GET", "store/v1/*");
        Endpoint ep1 = new Endpoint().withIncludeOn("GET", "catalog/[{collection}]/*");
        MockAction booksA = new MockAction().withIncludeOn("GET", "catalog/books/[{resource}]/[{relationship}]");
        api.withAction(booksA);
        api.withEndpoint(ep1);
        engine.withApi(api);

        List<Operation> ops = engine.buildOperations();

        System.out.println("----");
        for(Operation op : ops)
            System.out.println(op.getMethod() + " " + op.getDisplayPath());
        System.out.println("----");

        assertEquals(3, ops.size());
        assertOp(ops, "GET", "servlet/store/v1/catalog/books");
        assertOp(ops, "GET", "servlet/store/v1/catalog/books/{resource}");
        assertOp(ops, "GET", "servlet/store/v1/catalog/books/{resource}/{relationship}");
    }
//
//    @Test
//    public void buildAllOperationPaths_apiActionDeclaresFullPath(){
//        Engine engine = new Engine().withIncludeOn("GET", "servlet/*");
//        Api api = new Api().withIncludeOn("GET", "store/v1/*");
//        Endpoint ep1 = new Endpoint().withIncludeOn("GET", "catalog/[{collection}]/*");
//        MockAction booksA = new MockAction().withIncludeOn("GET", "catalog/books/[{resource}]/[{relationship}]");
//        api.withEndpoint(ep1);
//        api.withAction(booksA);
//
//        List<Path> paths = api.buildAllOperationPaths(engine);
//        assertEquals("[servlet/store/v1/catalog/books/[{resource}]/[{relationship}]]", paths.toString());
//    }
//
//    @Test
//    public void buildAllOperationPaths_MultipleEpActions(){
//        Engine engine = new Engine().withIncludeOn("GET", "servlet/*");
//        Api api = new Api().withIncludeOn("GET", "store/v1/*");
//        Endpoint ep1 = new Endpoint().withIncludeOn("GET", "catalog/[{collection}]/*");
//        MockAction booksA = new MockAction().withIncludeOn("GET", "/books/[{resource}]");
//        MockAction catsA = new MockAction().withIncludeOn("GET", "/categories/[catId]");
//        ep1.withActions(booksA, catsA);
//
//        MockAction carsA = new MockAction().withIncludeOn("GET", "catalog/cars/[carId]");
//        api.withEndpoint(ep1);
//        api.withAction(booksA);
//
//        List<Path> paths = api.buildAllOperationPaths(engine);
//        assertEquals("[servlet/store/v1/catalog/books/[{resource}], servlet/store/v1/catalog/categories/[catId]]", paths.toString());
//    }
//
//    @Test
//    public void buildAllOperationPaths_MultipleEpActions2(){
//        Engine engine = new Engine().withIncludeOn("GET", "servlet/*");
//        Api api = new Api().withIncludeOn("GET", "store/v1/*");
//        Endpoint ep1 = new Endpoint().withIncludeOn("GET", "catalog/[{collection}]/*");
//        MockAction booksA = new MockAction().withIncludeOn("GET", "/books/[{resource}]");
//        MockAction booksB = new MockAction().withIncludeOn("GET", "/books/[{bookId}]/[relationship]");
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
//        Api api = new Api().withIncludeOn("*", "test/{var1}/*").withEndpoint("*", "ep1/{var1}/*", new Action()).withCollection(new Collection().withName("test"));
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
//        Api api = new Api().withIncludeOn("*", "test/{var1}/*").withEndpoint("*", "ep1/{var1}/*", new Action()).withCollection(new Collection().withName("test"));
//
//        try{
//            api.getOperations();
//            fail("Should have thrown an error because variable var1 is mismatched");
//        }
//        catch(Exception ex){
//            System.out.println(ex.getMessage());
//        }
//    }


    @Test
    public void temp(){
        //String regex = "(?=[0-9]*)(?=[+-]?([0-9]*[.])?[0-9]+)";
        String regex = "(?=[a-z0-9]+)";
        RgxGen        rgxGen        = new RgxGen(regex);
        StringIterator uniqueStrings = rgxGen.iterateUnique();
        String         match         = uniqueStrings.next();
        if (match.length() == 0)
            match = uniqueStrings.next();
        System.out.println(match);
    }


}
