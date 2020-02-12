package graphql.nadel.result;


import java.time.Duration;
import java.time.OffsetDateTime;

public class ElapsedTime {

    private final OffsetDateTime startTime;
    private final long elapsedNanos;

    public ElapsedTime(OffsetDateTime startTime, long elapsedNanos) {
        this.startTime = startTime;
        this.elapsedNanos = elapsedNanos;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public long getElapsedNanos() {
        return elapsedNanos;
    }

    public static Builder newElapsedTime() {
        return new Builder();
    }

    public static class Builder {

        private OffsetDateTime start;
        private long elapsedNanos;

        public synchronized Builder start() {
            start = OffsetDateTime.now();
            return this;
        }

        public synchronized Builder stop() {
            OffsetDateTime stop = OffsetDateTime.now();
            elapsedNanos = Duration.between(start, stop).toNanos();
            return this;
        }

        public synchronized ElapsedTime build() {
            return new ElapsedTime(start, elapsedNanos);
        }
    }
}
