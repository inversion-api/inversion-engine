package io.inversion.jdbc;

import io.inversion.*;
import io.inversion.action.db.DbAction;
import io.inversion.utils.JSArray;
import io.inversion.utils.JSNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
                .withEndpoint("*", "*/", new DbAction())//
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
    public void get_encodedCompoundResourceKeyAsForeignKey() {
        Response res;

        res = engine.get("person/persons?expands=props").dump();
        assertEquals("employee~12345", res.find("data.0.props.0.personresourcekey"));

        res = engine.get("person/props?expands=person").dump();
        assertEquals("http://localhost/person/persons/employee~12345", res.find("data.0.person.href"));

        res = engine.get("person/persons/employee~12345/props").dump();
        assertEquals("employee~12345", res.find("data.0.personresourcekey"));

        //      JSArray data = engine.get("person/persons?expands=props").data();
        //      data.findAll("**.*").forEach(node -> {if (node instanceof JSNode && !(node instanceof JSArray)){((JSNode) node).removeAll("href", "person", "personresourcekey", "reportsto");}});

        //System.out.println(data);

        //-- insert with the ONE_TO_MANY parent as the json parent
        JSNode newPerson = new JSNode("type", "employee", "Identifier", "33333", "props", new JSArray(new JSNode("name", "testProp1", "value", "testValue1")));
        engine.post("person/persons", newPerson).dump();
        res = engine.get("person/persons/employee~33333?expands=props").dump();
        assertEquals("http://localhost/person/persons/employee~33333", res.find("data.0.props.0.person"));

        //-- insert with the MANY_TO_ONE child as the json parent
        JSNode newProp = new JSNode("name", "testProp2", "value", "testValue2", "person", new JSNode("type", "employee", "Identifier", "4444"));
        res = engine.post("person/props", newProp).assertOk();

        String newUrl = res.findString("data.0.href");
        res = engine.get(newUrl + "?expands=person").assertOk();

        res = engine.get("http://localhost/person/persons/employee~4444?expands=props").assertOk();
        assertEquals("http://localhost/person/persons/employee~4444", res.find("data.0.props.0.person"));

    }

}
