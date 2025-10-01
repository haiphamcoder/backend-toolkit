package io.github.haiphamcoder.toolkit.core.id;

import java.util.UUID;

/**
 * @author haiphamcoder
 * @since 1.0.0
 * @version 1.0.0
 * @see IdGenerator
 * @see UUID
 * @see UUID#randomUUID()
 * @see UUID#toString()
 * @see <a href="https://www.rfc-editor.org/rfc/rfc4122">RFC 4122</a>
 */
public final class UuidGenerator implements IdGenerator {

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateId() {
        return UUID.randomUUID().toString();
    }

}
