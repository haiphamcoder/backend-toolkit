package io.github.haiphamcoder.toolkit.core.id;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class UuidGeneratorTest {

    @Test
    void generateId_shouldProduceValidUuidAndBeUnique() {
        IdGenerator generator = new UuidGenerator();

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            String id = generator.generateId();
            assertNotNull(id);
            assertDoesNotThrow(() -> UUID.fromString(id));
            assertTrue(seen.add(id), "Duplicate UUID generated");
        }
    }
}
