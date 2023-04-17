package io.inversion.jdbc;

import io.inversion.*;
import io.inversion.action.db.DbAction;
import io.inversion.action.hateoas.HALFilter;
import io.inversion.spring.main.InversionMain;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.sql.Connection;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestShardedCollections {
//
//    Engine engine = null;
//    Api    api    = null;
//    Db     db     = null;
//
//    public static void main(String[] args)throws Exception{
//
//        Api api = new TestShardedCollections().buildApi();
//        InversionMain.run(api);
//    }
//
//    Api buildApi(){
//        String ddlUrl = JdbcDb.class.getResource("inventory-h2.ddl").toString();
//        Db db = new JdbcDb("h2", //
//                "org.h2.Driver", //
//                "jdbc:h2:mem:" + getClass().getSimpleName() + ";IGNORECASE=TRUE;DB_CLOSE_DELAY=-1", //
//                "sa", //
//                "", //
//                ddlUrl);
//
//        DbAction dbAction = new DbAction();
//
//        Api api = new Api()//
//                .withName("pricebook")//
//                .withDb(db)//
//                .withAction(new HALFilter().withOrder(50))
//                //.withEndpoint("{storeId:[0-9]*}/[{_collection:items}]/[{productId}]/*", dbAction)
//                .withEndpoint("{storeId:[0-9]*}/[items]/*", dbAction)
//                .withEndpoint("[{_collection:products|stores|channels}]/*", dbAction)
//        ;
//
//        db.withIncludeTables("Product|products");//, "Channel:Store");
//
//
//        /**
//         * stores/
//         * products/
//         * channels/
//         * {storeId}/items/[{productId}]  //-- don't use "pricebook/products/{storeId}/{productId}" unless you want to make pricebook/products queryable...which would be much harder to support pagination etc.
//         * {storeId}/inventory/[{productId}]
//         * {storeId}/channels/[{productId}]
//         *
//         *
//         *
//         */
//
//        api.withApiListener(new Api.ApiListener() {
//            @Override
//            public void onStartup(Engine engine, Api api) {
//                Collection items = new Collection("items") {
//                    @Override
//                    public String getTableName() {
//                        return "StoreProducts_1234_20210301";
//                    }
//                };
//                //items.withIncludeOn("{storeId:[0-9]*}/[{_collection:items}]/[{_entity}]/[{_relationship}]/*");
//                items.withDb(db);
//                //items.withProperty("storeId", "string");
//                items.withProperty("productId", "string");
//                items.withProperty("price", "number");
//                items.withProperty("inventory", "number");
//                items.withIndex("pk", "primary", true, "productId");
//                api.withCollection(items);
//
//                items.withManyToOneRelationship("product", api.getCollection("products"), "productId");
//
//                Collection products = api.getCollection("products");
//                products.withOneToManyRelationship("items", items, "productId");
//
//
//                Connection conn = ((JdbcDb) db).getConnection();
//
//                String sql = "";
//                sql += "SELECT \"StoreProducts_1234_20210301\".*  ";
//                sql += "FROM \"StoreProducts_1234_20210301\" ";
//                sql += " WHERE EXISTS (SELECT 1 FROM \"Product\" \"~~relTbl_Product\" WHERE \"StoreProducts_1234_20210301\".\"productId\" = \"~~relTbl_Product\".\"productId\" AND \"~~relTbl_Product\".\"name\" = '1234') ";
//                sql += "ORDER BY \"StoreProducts_1234_20210301\".\"productId\" ASC ";
//                sql += " LIMIT 100 OFFSET 0";
//
//                try {
//
//                    conn.createStatement().execute(sql);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//        return api;
//
//    }
//
//    @BeforeAll
//    public void beforeAll_initializeEngine() {
//        Chain.resetAll();
//        JdbcConnectionLocal.closeAll();
//        buildApi();
//        if(engine != null)
//            engine.startup();
//
//    }
//
//    @AfterAll
//    public void afterAll_finalizeEngine() {
//        if (db != null) {
//            db.shutdown();
//        }
//    }
//
//    @Test
//    public void testShardedCollections() throws Exception {
//        Response resp = null;
//        resp = engine.get("pricebook/stores/");
//        resp.dump();
//    }
}