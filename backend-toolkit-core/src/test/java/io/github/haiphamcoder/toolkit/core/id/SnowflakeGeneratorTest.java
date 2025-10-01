package io.github.haiphamcoder.toolkit.core.id;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SnowflakeGeneratorTest {

    @Test
    void nextId_shouldIncreaseAndBeUnique() {
        SnowflakeGenerator generator = new SnowflakeGenerator(0, 0, 1577836800000L);

        Set<Long> seen = new HashSet<>();
        long prev = generator.nextLong();
        seen.add(prev);
        for (int i = 0; i < 10_000; i++) {
            long next = generator.nextLong();
            assertTrue(next > prev, "Snowflake should be strictly increasing in same process");
            assertTrue(seen.add(next), "Duplicate Snowflake generated");
            prev = next;
        }
    }

    @Test
    void generateId_shouldBeUnsignedDecimalString() {
        SnowflakeGenerator generator = new SnowflakeGenerator(1, 1, 1577836800000L);
        String id = generator.generateId();
        assertNotNull(id);
        assertTrue(id.matches("^[0-9]+$"));
        // parse as unsigned to verify round-trip
        long raw = generator.nextLong();
        String asString = Long.toUnsignedString(raw);
        assertTrue(asString.matches("^[0-9]+$"));
    }

    @Test
    void constructor_shouldValidateBounds() {
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeGenerator(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeGenerator(0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeGenerator(32, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeGenerator(0, 32, 0));
    }
}
