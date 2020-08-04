package io.inversion;

import io.inversion.action.db.DbAction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public interface AbstractEngineTest extends AbstractDbTest {

    public Engine getEngine();

    public void setEngine(Engine engine);

    public String getType();
   
    public Db buildDb();

    @BeforeAll
    public default void beforeAll_initializeEngine() {
        Db db = buildDb();
        if(isIntegTest())
            db.withDryRun(false);
        else
            db.withDryRun(true);
        
        Engine engine = new Engine().withApi(new Api("northwind") //
                                                                 .withEndpoint("*", db.getType() + "/*", new DbAction())//
                                                                 .withDb(db));
        setEngine(engine);
    }

    @AfterAll
    public default void afterAll_finalizeEngine() {
        Engine engine = getEngine();
        if (engine != null)
            engine.shutdown();

        setEngine(null);

        Chain.resetAll();
    }

    public default boolean isIntegTest() {
        return getClass().getSimpleName().indexOf("IntegTest") > -1 || (getClass().getSimpleName().indexOf("UnitTest") < 0 && getClass().getSimpleName().indexOf("H2") > -1);
    }

}
