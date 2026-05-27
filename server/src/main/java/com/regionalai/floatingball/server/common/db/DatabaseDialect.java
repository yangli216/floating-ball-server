package com.regionalai.floatingball.server.common.db;

import com.baomidou.mybatisplus.annotation.DbType;

public interface DatabaseDialect {

    DatabaseType type();

    String databaseId();

    DbType mybatisPlusDbType();

    String dayLabelExpression(String columnName);
}
