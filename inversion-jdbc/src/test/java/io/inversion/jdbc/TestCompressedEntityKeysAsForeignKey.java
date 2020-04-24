package io.inversion.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.inversion.Api;
import io.inversion.Collection;
import io.inversion.Db;
import io.inversion.Engine;
import io.inversion.Response;
import io.inversion.action.rest.RestAction;
import io.inversion.utils.JSArray;
import io.inversion.utils.JSNode;

/**
 * Tests that a compound primary key (multiple fields) can be foreign key referenced by
 * the encoded entity key itself when the foreign key index only has a single value.
 * This feature was developed to allow document stores to overload a single table
 * to hold multiple entity types by supplying a "type" property as part of the 
 * primary index.  
 * 
 * 
 * TODO: it would be nice to create northwind tables for all schemas with this
 * encoded entity key structure
 */
public class TestCompressedEntityKeysAsForeignKey
{
   @Test
   public void get_encodedCompoundEntityKeyAsForeignKey()
   {
      Db db = new JdbcDb("db", "org.h2.Driver", //
                         "jdbc:h2:mem:" + "get_compressedCompoundForeignKey" + ";IGNORECASE=TRUE;DB_CLOSE_DELAY=-1", //
                         "sa", //
                         "", //
                         JdbcDb.class.getResource("person-h2.ddl").toString());

      Api api = new Api("person") //
                                 .withEndpoint("*", "*/", new RestAction())//
                                 .withDb(db);

      Engine engine = new Engine().withApi(api);
      engine.startup();

      api.withRelationship("persons", "props", "props", "person", "personEntityKey");

      Response res = null;

      res = engine.get("person/persons?expands=props").dump();
      assertEquals("employee~12345", res.find("data.0.props.0.personentitykey"));

      res = engine.get("person/props?expands=person").dump();
      assertEquals("http://localhost/person/persons/employee~12345", res.find("data.0.person.href"));

      res = engine.get("person/persons/employee~12345/props").dump();
      assertEquals("employee~12345", res.find("data.0.personentitykey"));

      //      JSArray data = engine.get("person/persons?expands=props").data();
      //      data.findAll("**.*").forEach(node -> {if (node instanceof JSNode && !(node instanceof JSArray)){((JSNode) node).removeAll("href", "person", "personentitykey", "reportsto");}});

      //System.out.println(data);

      //-- insert with the ONE_TO_MANY parent as the json parent
      JSNode newPerson = new JSNode("type", "employee", "Identifier", "33333", "props", new JSArray(new JSNode("name", "testProp1", "value", "testValue1")));
      res = engine.post("person/persons", newPerson).dump();
      res = engine.get("person/persons/employee~33333?expands=props").dump();
      assertEquals("http://localhost/person/persons/employee~33333", res.find("data.0.props.0.person"));

      //-- insert with the MANY_TO_ONE child as the json parent
      JSNode newProp = new JSNode("name", "testProp2", "value", "testValue2", "person", new JSNode("type", "employee", "Identifier", "4444"));
      res = engine.post("person/props", newProp).dump().assertOk();

      String newUrl = res.findString("data.0.href");
      res = engine.get(newUrl + "?expands=person").dump().assertOk();

      res = engine.get("http://localhost/person/persons/employee~4444?expands=props").dump().assertOk();
      assertEquals("http://localhost/person/persons/employee~4444", res.find("data.0.props.0.person"));

   }
}
