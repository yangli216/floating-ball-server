package com.regionalai.floatingball.server.modules.adminui.controller;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class AdminUiResourcePackagingTest {

    @Test
    void adminIndexShouldBeAvailableOnClasspath() {
        ClassPathResource resource = new ClassPathResource("static/admin/index.html");

        assertThat(resource.exists()).isTrue();
    }
}