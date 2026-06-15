package com.regionalai.floatingball.server.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mybatis-plus")
public class MybatisPlusDatabaseProperties {

    /**
     * Database dialect used by MyBatis-Plus pagination and local SQL helpers.
     */
    private String dbType = "oracle";
}
