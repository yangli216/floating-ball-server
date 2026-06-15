package com.regionalai.floatingball.server.common.db;

import com.regionalai.floatingball.server.common.config.MybatisPlusDatabaseProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseDialectTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
        .withUserConfiguration(MybatisPlusDatabaseProperties.class, DatabaseDialect.class);

    @Test
    void shouldResolveGaussdbAliasesAsPgCompatible() {
        MybatisPlusDatabaseProperties properties = new MybatisPlusDatabaseProperties();
        properties.setDbType("gaussdb");

        DatabaseDialect dialect = new DatabaseDialect(properties);

        assertTrue(dialect.isPgCompatible());
        assertEquals("LIMIT 3", dialect.firstRows(3));
        assertEquals("COALESCE(a, b)", dialect.nvl("a", "b"));
        assertTrue(dialect.dayText("insert_time").contains("::date"));
    }

    @Test
    void shouldDefaultToOracleDialect() {
        MybatisPlusDatabaseProperties properties = new MybatisPlusDatabaseProperties();

        DatabaseDialect dialect = new DatabaseDialect(properties);

        assertFalse(dialect.isPgCompatible());
        assertEquals("FETCH FIRST 1 ROWS ONLY", dialect.firstRows(1));
        assertEquals("NVL(a, b)", dialect.nvl("a", "b"));
        assertTrue(dialect.dayText("insert_time").contains("TRUNC"));
    }

    @Test
    void shouldWireDialectBeanFromDatabaseProperties() {
        contextRunner
            .withPropertyValues("mybatis-plus.db-type=opengauss")
            .run(context -> {
                DatabaseDialect dialect = context.getBean(DatabaseDialect.class);

                assertEquals(DatabaseDialect.Kind.OPENGAUSS, dialect.getKind());
                assertTrue(dialect.isPgCompatible());
            });
    }
}
