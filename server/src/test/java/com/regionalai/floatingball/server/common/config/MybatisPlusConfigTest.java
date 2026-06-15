package com.regionalai.floatingball.server.common.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MybatisPlusConfigTest {

    @Test
    void shouldCreateInterceptorForGaussdbProfile() {
        MybatisPlusDatabaseProperties properties = new MybatisPlusDatabaseProperties();
        properties.setDbType("opengauss");

        MybatisPlusInterceptor interceptor = new MybatisPlusConfig(properties).mybatisPlusInterceptor();

        assertNotNull(interceptor);
    }
}
