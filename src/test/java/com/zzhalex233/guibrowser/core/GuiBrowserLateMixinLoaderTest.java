package com.zzhalex233.guibrowser.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class GuiBrowserLateMixinLoaderTest {

    @Test
    void mixinConfigResourceExists() {
        assertNotNull(getClass().getClassLoader().getResource("mixins.guibrowser.json"));
    }
}