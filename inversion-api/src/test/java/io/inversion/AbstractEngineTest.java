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
        if (isIntegTest())
            db.withDryRun(false);
        else
            db.withDryRun(true);

        Engine engine = new Engine().withApi(buildApi(db));
        setEngine(engine);
    }

    public default Api buildApi(Db db) {

        return buildDefaultApi(db);
    }
    
    public default Api buildDefaultApi(Db db) {

        return new Api("northwind") //
                                   .withEndpoint("*", db.getType() + "/*", new DbAction())//
                                   .withDb(db);
    }
    

    public default void configureDefaultModel(Db db) {
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
        employees.withIndex("fkIdx_Employees_reportsTo", "FOREIGN_KEY", false, "reportsTo");

        employees.withRelationship(new Relationship("reportsTo", Relationship.REL_MANY_TO_ONE, employees, employees, employees.getIndex("fkIdx_Employees_reportsTo"), null));
        employees.withRelationship(new Relationship("employees", Relationship.REL_ONE_TO_MANY, employees, employees, employees.getIndex("fkIdx_Employees_reportsTo"), null));

        Collection employeeOrderDetails = new Collection("employeeOrderDetails").withProperty("employeeId", "INTEGER")//
                                                                                .withProperty("orderId", "INTEGER")//
                                                                                .withProperty("productId", "INTEGER")//
                                                                                .withIndex("PK_EmployeeOrderDetails", "primary", true, "employeeId", "orderId", "productId");

        employeeOrderDetails.getProperty("employeeId").withPk(employees.getProperty("employeeId"));
        employeeOrderDetails.getProperty("orderId").withPk(orderDetails.getProperty("orderId"));
        employeeOrderDetails.getProperty("productId").withPk(orderDetails.getProperty("productId"));

        employeeOrderDetails.withIndex("FK_EOD_employeeId", "FOREIGN_KEY", false, "employeeId");
        employeeOrderDetails.withIndex("FK_EOD_orderdetails", "FOREIGN_KEY", false, "orderId", "productId");

        employees.withRelationship(new Relationship("orderdetails", Relationship.REL_MANY_TO_MANY, employees, orderDetails, employeeOrderDetails.getIndex("FK_EOD_employeeId"), employeeOrderDetails.getIndex("FK_EOD_orderdetails")));

        db.withCollections(orders, orderDetails, employees, employeeOrderDetails);
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
