package io.inversion.dynamodb;

import io.inversion.*;
import io.inversion.utils.Url;

public interface AbstractDynamoTest extends AbstractEngineTest {

    default Api buildApi(Db db) {
        Api api = buildDefaultApi(db);
        if (isIntegTest()) {

            api.withAction(new Action() {

                public int getOrder() {
                    return 1;
                }

                public void run(Request req, Response res) throws ApiException {

                    Url    url     = req.getUrl();
                    String typeKey = url.findKey("type");
                    if (typeKey == null) {
                        if ("orders".equalsIgnoreCase(req.getCollectionKey())) {
                            url.withParam("type", "ORDER");
                        }
                    }
                }
            });
        }
        return api;
    }

    @Override
    default Db buildDb() {
        Db db = new DynamoDb().withName("bad_name_missing_env_props_on_purpose");
        if (isIntegTest()) {
            db = DynamoDbFactory.buildNorthwindDynamoDb();
        } else {
            configureDefaultModel(db);

            Collection orders = db.getCollectionByTableName("orders");
            orders.withProperty("type", "S");

            Collection orderDetails         = db.getCollectionByTableName("orderDetails");
            Collection employees            = db.getCollectionByTableName("employees");
            Collection employeeOrderDetails = db.getCollectionByTableName("employeeOrderDetails");

            for (Index index : orders.getIndexes())
                orders.removeIndex(index);

            for (Index index : orderDetails.getIndexes())
                orderDetails.removeIndex(index);

            for (Index index : employees.getIndexes())
                employees.removeIndex(index);

            for (Index index : employeeOrderDetails.getIndexes())
                employeeOrderDetails.removeIndex(index);

            for (Relationship r : orders.getRelationships())
                orders.removeRelationship(r);

            for (Relationship r : orderDetails.getRelationships())
                orderDetails.removeRelationship(r);

            for (Relationship r : employees.getRelationships())
                employees.removeRelationship(r);

            for (Relationship r : employeeOrderDetails.getRelationships())
                employeeOrderDetails.removeRelationship(r);

            orders.withIndex(DynamoDb.PRIMARY_INDEX_NAME, DynamoDb.PRIMARY_INDEX_TYPE, true, "orderId", "type");
            orders.withIndex("ls1", DynamoDb.LOCAL_SECONDARY_INDEX_TYPE, false, "orderId", "shipCity");
            orders.withIndex("ls2", DynamoDb.LOCAL_SECONDARY_INDEX_TYPE, false, "orderId", "shipName");
            orders.withIndex("ls3", DynamoDb.LOCAL_SECONDARY_INDEX_TYPE, false, "orderId", "requiredDate");
            orders.withIndex("gs1", DynamoDb.GLOBAL_SECONDARY_INDEX_TYPE, false, "employeeId", "orderDate");
            orders.withIndex("gs2", DynamoDb.GLOBAL_SECONDARY_INDEX_TYPE, false, "customerId", "requiredDate");
            orders.withIndex("gs3", DynamoDb.GLOBAL_SECONDARY_INDEX_TYPE, false, "type", "orderId");

            orderDetails.withIndex(DynamoDb.PRIMARY_INDEX_NAME, DynamoDb.PRIMARY_INDEX_TYPE, true, "orderId", "productId");
            orderDetails.getProperty("orderId").withPk(orders.getProperty("orderId"));

            employees.withProperty("type", "S");
            employees.withIndex(DynamoDb.PRIMARY_INDEX_NAME, DynamoDb.PRIMARY_INDEX_TYPE, true, "employeeId", "type");
            employees.getProperty("reportsTo").withPk(employees.getProperty("employeeId"));
            employees.withIndex("fkIdx_Employees_reportsTo", "FOREIGN_KEY", false, "reportsTo");
            employees.withRelationship(new Relationship("reportsTo", Relationship.REL_MANY_TO_ONE, employees, employees, employees.getIndex("fkIdx_Employees_reportsTo"), null));
            employees.withRelationship(new Relationship("employees", Relationship.REL_ONE_TO_MANY, employees, employees, employees.getIndex("fkIdx_Employees_reportsTo"), null));

            employeeOrderDetails.withIndex(DynamoDb.PRIMARY_INDEX_NAME, DynamoDb.PRIMARY_INDEX_TYPE, true, "employeeId", "orderId", "productId");
            employeeOrderDetails.getProperty("employeeId").withPk(employees.getProperty("employeeId"));
            employeeOrderDetails.getProperty("orderId").withPk(orderDetails.getProperty("orderId"));
            employeeOrderDetails.getProperty("productId").withPk(orderDetails.getProperty("productId"));

            employeeOrderDetails.withIndex("FK_EOD_employeeId", "FOREIGN_KEY", false, "employeeId");
            employeeOrderDetails.withIndex("FK_EOD_orderdetails", "FOREIGN_KEY", false, "orderId", "productId");

            employees.withRelationship(new Relationship("orderdetails", Relationship.REL_MANY_TO_MANY, employees, orderDetails, employeeOrderDetails.getIndex("FK_EOD_employeeId"), employeeOrderDetails.getIndex("FK_EOD_orderdetails")));

            db.withCollections(orders, orderDetails, employees, employeeOrderDetails);
        }

        return db;
    }

}
