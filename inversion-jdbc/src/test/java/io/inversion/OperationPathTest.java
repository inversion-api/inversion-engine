package io.inversion;

import io.inversion.action.db.DbAction;
import io.inversion.jdbc.JdbcConnectionLocal;
import io.inversion.jdbc.JdbcDbFactory;
import io.inversion.utils.Path;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.junit.jupiter.api.Test;

import java.util.List;


public class OperationPathTest {

    public void test_DbAction_getOperationPaths(){

        Engine engine = new Engine();

        JdbcConnectionLocal.closeAll();
        Chain.resetAll();

        Db db = JdbcDbFactory.buildDb("h2", getClass().getSimpleName());
        Action action = new DbAction();
        Endpoint ep = new Endpoint("*", "/*", new DbAction());

        Api api = new Api();
        api.withDbs(db);
        api.withEndpoint(ep);

        engine.withApi(api);
        engine.startup();

        List<Path> actionPaths = action.getOperationPaths("GET", api);
        System.out.println(actionPaths);
    }


    @Test
    public void test_northwind() {
        Engine engine = new Engine();

        JdbcConnectionLocal.closeAll();
        Chain.resetAll();

        Db db = JdbcDbFactory.buildDb("h2", getClass().getSimpleName());
        Action action = new DbAction();
        Endpoint ep = new Endpoint("*", "/*", new DbAction());

        Api api = new Api();
        api.withDbs(db);
        api.withEndpoint(ep);

        engine.withApi(api);
        engine.startup();

        ArrayListValuedHashMap operationPaths = action.getOperationPaths(api);
        System.out.println(operationPaths);

    }
}
