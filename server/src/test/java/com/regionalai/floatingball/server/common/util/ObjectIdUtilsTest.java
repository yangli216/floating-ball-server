package com.regionalai.floatingball.server.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectIdUtilsTest {

    @Test
    void nextReturnsTwentyFourHexCharacters() {
        String first = ObjectIdUtils.next();
        String second = ObjectIdUtils.next();

        assertEquals(24, first.length());
        assertEquals(24, second.length());
        assertTrue(first.matches("[0-9a-f]{24}"));
        assertTrue(second.matches("[0-9a-f]{24}"));
        assertNotEquals(first, second);
    }
}
