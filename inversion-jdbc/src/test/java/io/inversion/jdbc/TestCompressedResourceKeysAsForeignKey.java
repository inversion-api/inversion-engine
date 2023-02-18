package io.inversion.jdbc;

import io.inversion.*;
import io.inversion.action.db.DbAction;
import io.inversion.action.hateoas.LinksFilter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that a compound primary key (multiple fields) can be foreign key referenced by
 * the encoded resource key itself when the foreign key index only has a single value.
 * This feature was developed to allow document stores to overload a single table
 * to hold multiple resource types by supplying a "type" property as part of the
 * primary index.
 * <p>
 * <p>
 * TODO: it would be nice to create northwind tables for all schemas with this
 * encoded resource key structure
 */
@TestInstance(Lifecycle.PER_CLASS)
public class TestCompressedResourceKeysAsForeignKey {
    Engine engine = null;
    Api    api    = null;
    Db     db     = null;

    @BeforeAll
    public void beforeAll_initializeEngine() {
        Chain.resetAll();
        JdbcConnectionLocal.closeAll();

        db = JdbcDbFactory.bootstrapH2(getClass().getName() + System.currentTimeMillis(), JdbcDb.class.getResource("person-h2.ddl").toString());
        api = new Api("person") //
                .withEndpoint(new LinksFilter(), new DbAction())//
                .withDb(db);

        engine = new Engine(api);
        engine.startup();
        api.withRelationship("persons", "props", "props", "person", "personResourceKey");
    }

    @AfterAll
    public void afterAll_finalizeEngine() {
        if (db != null) {
            db.shutdown();
        }
    }

    @Test
    public void get_expand_one_to_many_compound_resource_key(){
        Response res;

        res = engine.get("person/persons?expands=props").dump();
        assertEquals("employee~12345", res.find("data.0.props.0.personresourcekey"));
    }


    @Test
    public void get_expand_many_to_one_compressed_resource_key(){
        Response res;

        res = engine.get("person/props?expands=person").dump();
        assertTrue(res.findString("data.0.person.href").endsWith("/employee~12345"));
    }

    @Test
    public void get_one_to_may_relationship_of_compressed_key(){
        Response res;

        res = engine.get("person/persons/employee~12345/props").dump();
        assertEquals("employee~12345", res.find("data.0.personresourcekey"));
        assertTrue(res.findString("data.0.person").endsWith("/employee~12345"));
    }

//    @Test
//    public void put_duplicate_no_changes(){
//        Response res;
//        res = engine.get("person/persons?expands=props");
//        res.dump().assertOk();
//
//        String toMatch = res.getJson().toString();
//
//        res = engine.post("person/persons", res.getData());
//        res.dump().assertOk();
//
//        res = engine.get("person/persons?expands=props");
//        res.dump().assertOk();
//
//        assertEquals(toMatch, res.getJson().toString());
//
//    }
//
//    @Test
//    public void post_with_one_to_many_with_child_compressed_foreign_key(){
//        Response res;
//
//        JSNode newPerson = new JSMap("type", "employee", "Identifier", "33333", "props", new JSList(new JSMap("name", "testProp1", "value", "testValue1")));
//        engine.post("person/persons", newPerson).dump();
//        res = engine.get("person/persons/employee~33333?expands=props").dump();
//        assertTrue(res.findString("data.0.props.0.person").endsWith("/employee~33333"));
//    }
//
//    @Test
//    public void post_many_to_one_with_parent_compressed_foreign_key(){
//        Response res;
//
//        JSNode newProp = new JSMap("name", "testProp2", "value", "testValue2", "person", new JSMap("type", "employee", "Identifier", "4444"));
//        res = engine.post("person/props", newProp).assertOk();
//        //todo add assertions about all parts of child pk
//        //todo add assertions about parent compressed fk
//    }
//
//
//    @Test
//    public void get_encodedCompoundResourceKeyAsForeignKey() {
//        Response res;
//
//        //-- insert with the MANY_TO_ONE child as the json parent
//        JSNode newProp = new JSMap("name", "testProp2", "value", "testValue2", "person", new JSMap("type", "employee", "Identifier", "4444"));
//        res = engine.post("person/props", newProp).assertOk();
//
//        String newUrl = res.findString("data.0.href");
//        res = engine.get(newUrl + "?expands=person").assertOk();
//
//        res = engine.get("http://localhost/person/persons/employee~4444?expands=props").assertOk();
//        assertTrue(res.findString("data.0.props.0.person").endsWith("/employee~4444"));
//
//    }

}
