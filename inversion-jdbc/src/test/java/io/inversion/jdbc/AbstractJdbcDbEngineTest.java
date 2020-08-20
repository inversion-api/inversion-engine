package io.inversion.jdbc;

import io.inversion.AbstractEngineTest;
import io.inversion.Chain;
import io.inversion.Db;

public interface AbstractJdbcDbEngineTest extends AbstractEngineTest {

    @Override
    public default Db buildDb() {
        JdbcConnectionLocal.closeAll();
        Chain.resetAll();

        if (isIntegTest() || getClass().getSimpleName().toLowerCase().contains("h2"))
            return JdbcDbFactory.buildDb(getType(), getClass().getSimpleName());
        else {
            Db db = new JdbcDb(getType()).withType(getType());
            configureDefaultModel(db);
            return db;
        }
    }

}
