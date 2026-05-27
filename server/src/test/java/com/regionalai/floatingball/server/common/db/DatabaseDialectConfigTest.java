package com.regionalai.floatingball.server.common.db;

import com.baomidou.mybatisplus.annotation.DbType;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseDialectConfigTest {

    private final DatabaseDialectConfig config = new DatabaseDialectConfig();

    @Test
    void databaseDialectShouldResolveMybatisPlusDbType() {
        DatabaseProperties oracle = new DatabaseProperties();
        oracle.setType("oracle");
        assertEquals(DbType.ORACLE, config.databaseDialect(oracle).mybatisPlusDbType());

        DatabaseProperties postgres = new DatabaseProperties();
        postgres.setType("postgres");
        assertEquals(DbType.POSTGRE_SQL, config.databaseDialect(postgres).mybatisPlusDbType());
    }

    @Test
    void databaseIdProviderShouldMapVendorNames() throws Exception {
        DatabaseIdProvider provider = config.databaseIdProvider();

        assertEquals("oracle", provider.getDatabaseId(dataSourceWithProductName("Oracle")));
        assertEquals("postgres", provider.getDatabaseId(dataSourceWithProductName("PostgreSQL")));
    }

    private DataSource dataSourceWithProductName(String name) throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn(name);
        return dataSource;
    }
}
