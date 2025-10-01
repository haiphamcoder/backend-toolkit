package io.github.haiphamcoder.toolkit.core.id;

/**
 * @author haiphamcoder
 * @since 1.0.0
 * @version 1.0.0
 * @see IdGenerator
 * @see <a href="https://github.com/twitter-archive/snowflake">Snowflake
 *      Algorithm</a>
 */
public final class SnowflakeGenerator implements IdGenerator {

    private static final long DEFAULT_EPOCH_MILLIS = 1577836800000L; // 2020-01-01T00:00:00Z
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS); // 31
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS); // 31
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS); // 4095

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private final long workerId;
    private final long datacenterId;
    private final long epochMillis;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeGenerator() {
        this(0, 0, DEFAULT_EPOCH_MILLIS);
    }

    public SnowflakeGenerator(long workerId, long datacenterId) {
        this(workerId, datacenterId, DEFAULT_EPOCH_MILLIS);
    }

    /**
     * Creates a generator with custom epoch.
     * 
     * @param datacenterId 0..31
     * @param workerId     0..31
     * @param epochMillis  custom epoch in milliseconds (must be >= 0 and <= current
     *                     time)
     */
    public SnowflakeGenerator(long datacenterId, long workerId, long epochMillis) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId out of range: " + workerId);
        }
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException("datacenterId out of range: " + datacenterId);
        }
        if (epochMillis < 0) {
            throw new IllegalArgumentException("epochMillis must be >= 0");
        }
        long now = currentTime();
        if (epochMillis > now) {
            throw new IllegalArgumentException("epochMillis must be <= current time");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.epochMillis = epochMillis;
    }

    @Override
    public String generateId() {
        return Long.toUnsignedString(nextLong());
    }

    /**
     * Returns raw 64-bit snowflake value.
     */
    public long nextLong() {
        return nextId();
    }

    public synchronized long nextId() {
        long timestamp = currentTime();

        if (timestamp < lastTimestamp) {
            // Clock moved backwards: block until we catch up to the last seen timestamp
            timestamp = waitUntil(lastTimestamp);
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - epochMillis) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long lastTs) {
        long ts = currentTime();
        while (ts <= lastTs) {
            ts = currentTime();
        }
        return ts;
    }

    private static long currentTime() {
        return System.currentTimeMillis();
    }

    private static long waitUntil(long targetTs) {
        long ts;
        do {
            ts = currentTime();
            // Hint to the CPU that we are in a spin-wait loop
            try {
                Thread.onSpinWait();
            } catch (NoSuchMethodError ignored) {
                // on older JDKs, onSpinWait may not be available; ignore
            }
        } while (ts < targetTs);
        return ts;
    }

    @Override
    public String toString() {
        return "SnowflakeGenerator{dc=" + datacenterId + ", worker=" + workerId + ", epoch=" + epochMillis + "}";
    }

    @Override
    public int hashCode() {
        int result = (int) (workerId ^ (workerId >>> 32));
        result = 31 * result + (int) (datacenterId ^ (datacenterId >>> 32));
        result = 31 * result + (int) (epochMillis ^ (epochMillis >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        SnowflakeGenerator that = (SnowflakeGenerator) obj;
        return workerId == that.workerId && datacenterId == that.datacenterId && epochMillis == that.epochMillis;
    }
}
