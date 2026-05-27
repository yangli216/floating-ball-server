package com.regionalai.floatingball.server.common.db;

import com.baomidou.mybatisplus.annotation.DbType;

public class PostgresDatabaseDialect implements DatabaseDialect {

    @Override
    public DatabaseType type() {
        return DatabaseType.POSTGRES;
    }

    @Override
    public String databaseId() {
        return type().value();
    }

    @Override
    public DbType mybatisPlusDbType() {
        return DbType.POSTGRE_SQL;
    }

    @Override
    public String dayLabelExpression(String columnName) {
        return "TO_CHAR(" + columnName + ", 'YYYY-MM-DD')";
    }
}
