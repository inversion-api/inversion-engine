package io.inversion;

import io.inversion.action.db.DbAction;
import io.inversion.action.hateoas.LinksAction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;

public interface AbstractEngineTest extends AbstractDbTest {

    static boolean isIntegEnv() {
        String profile = System.getProperty("test.profile");
        if (profile == null)
            profile = System.getenv("test.profile");

        return profile != null && profile.contains("integ");
    }

    Engine getEngine();

    void setEngine(Engine engine);

    String getType();

    Db buildDb();

    @BeforeAll
    default void checkIntegEnv() {
        Assumptions.assumeTrue(shouldRun());
    }

    @BeforeAll
    default void beforeAll_initializeEngine() {
        Db db = buildDb();
        db.withDryRun(!isIntegTest() && !getClass().getSimpleName().toLowerCase().contains("h2"));

        Engine engine = new Engine().withApi(buildApi(db));
        setEngine(engine);
        //engine.startup();
    }

    default Api buildApi(Db db) {

        return buildDefaultApi(db);
    }

    default Api buildDefaultApi(Db db) {

        return new Api("northwind") //
                .withIncludeOn("northwind/*")
                .withEndpoint("" + db.getType() + "/*", new LinksAction(), new DbAction())//
                .withDb(db);
    }

    default void configureDefaultModel(Db db) {
        Collection orders = new Collection("orders").withProperty("orderId", "VARCHAR")//
                .withProperty("customerId", "INTEGER")//
                .withProperty("employeeId", "INTEGER")//
                .withProperty("orderDate", "DATETIME")//
                .withProperty("requiredDate", "DATETIME")//
                .withProperty("shippedDate", "DATETIME")//
                .withProperty("shipVia", "INTEGER")//
                .withProperty("freight", "DECIMAL")//
                .withProperty("shipName", "VARCHAR")//
                .withProperty("shipAddress", "VARCHAR")//
                .withProperty("shipCity", "VARCHAR")//
                .withProperty("shipRegion", "VARCHAR")//
                .withProperty("shipPostalCode", "VARCHAR")//
                .withProperty("shipCountry", "VARCHAR")//
                .withIndex("PK_Orders", "primary", true, "orderId");

        Collection orderDetails = new Collection("orderDetails").withProperty("employeeId", "INTEGER")//
                .withProperty("orderId", "INTEGER")//
                .withProperty("productId", "INTEGER")//
                .withProperty("quantity", "INTEGER")//
                .withIndex("PK_orderDetails", "primary", true, "orderId", "productId");

        orderDetails.getProperty("orderId").withPk(orders.getProperty("orderId"));

        Collection employees = new Collection("employees").withProperty("employeeId", "INTEGER")//
                .withProperty("firstName", "VARCHAR")//
                .withProperty("lastName", "VARCHAR")//
                .withProperty("reportsTo", "INTEGER")//
                .withIndex("PK_Employees", "primary", true, "employeeId");

        employees.getProperty("reportsTo").withPk(employees.getProperty("employeeId"));
        employees.withIndex("fkIdx_Employees_reportsTo", Index.TYPE_FOREIGN_KEY, false, "reportsTo");

        employees.withRelationship(new Relationship("reportsTo", Relationship.REL_MANY_TO_ONE, employees, employees, employees.getIndex("fkIdx_Employees_reportsTo"), null));
        employees.withRelationship(new Relationship("employees", Relationship.REL_ONE_TO_MANY, employees, employees, employees.getIndex("fkIdx_Employees_reportsTo"), null));

        Collection employeeOrderDetails = new Collection("employeeOrderDetails").withProperty("employeeId", "INTEGER")//
                .withProperty("orderId", "INTEGER")//
                .withProperty("productId", "INTEGER")//
                .withIndex("PK_EmployeeOrderDetails", "primary", true, "employeeId", "orderId", "productId");

        employeeOrderDetails.getProperty("employeeId").withPk(employees.getProperty("employeeId"));
        employeeOrderDetails.getProperty("orderId").withPk(orderDetails.getProperty("orderId"));
        employeeOrderDetails.getProperty("productId").withPk(orderDetails.getProperty("productId"));

        employeeOrderDetails.withIndex("FK_EOD_employeeId", Index.TYPE_FOREIGN_KEY, false, "employeeId");
        employeeOrderDetails.withIndex("FK_EOD_orderdetails", Index.TYPE_FOREIGN_KEY, false, "orderId", "productId");

        employees.withRelationship(new Relationship("orderdetails", Relationship.REL_MANY_TO_MANY, employees, orderDetails, employeeOrderDetails.getIndex("FK_EOD_employeeId"), employeeOrderDetails.getIndex("FK_EOD_orderdetails")));

        db.withCollections(orders, orderDetails, employees, employeeOrderDetails);
    }

    @AfterAll
    default void afterAll_finalizeEngine() {
        Engine engine = getEngine();
        if (engine != null)
            engine.shutdown();

        setEngine(null);

        Chain.resetAll();
    }

    default boolean shouldRun() {
        boolean run = !isIntegTest() || isIntegEnv();
        if (!run)
            System.out.println("SKIPPING INTEGRATION TEST: " + getClass().getSimpleName() + " because env or sys prop 'test.profile' is not 'integration'");

        return run;
    }

    default boolean isIntegTest() {

        String cn = getClass().getSimpleName().toLowerCase();
        if (cn.contains("h2"))
            return false;

        if (cn.contains("unittest"))
            return false;

        return cn.contains("integtest");
    }

}
