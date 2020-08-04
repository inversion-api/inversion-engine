package io.inversion.jdbc;

import io.inversion.AbstractDbTest;
import io.inversion.AbstractEngineTest;
import io.inversion.Chain;
import io.inversion.Db;

public interface AbstractJdbcDbEngineTest extends AbstractEngineTest {

    @Override
    public default Db buildDb() {
        JdbcConnectionLocal.closeAll();
        Chain.resetAll();

        if (isIntegTest())
            return JdbcDbFactory.buildDb(getType(), getClass().getSimpleName());
        else
            return new JdbcDb(getType()).withType(getType());
    }
}
