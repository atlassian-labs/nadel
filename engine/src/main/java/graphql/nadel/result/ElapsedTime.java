package graphql.nadel.result;


import graphql.Internal;

import java.time.Duration;
import java.time.OffsetDateTime;

@Internal
public class ElapsedTime {

    private final OffsetDateTime startTime;
    private final Duration duration;

    public ElapsedTime(OffsetDateTime startTime, Duration duration) {
        this.startTime = startTime;
        this.duration = duration;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public static Builder newElapsedTime() {
        return new Builder();
    }

    public Duration getDuration() {
        return duration;
    }

    public static class Builder {

        private OffsetDateTime start;
        private Duration duration;

        public synchronized Builder start() {
            start = OffsetDateTime.now();
            return this;
        }

        public synchronized Builder stop() {
            OffsetDateTime stop = OffsetDateTime.now();
            duration = Duration.between(start, stop);
            return this;
        }

        public synchronized ElapsedTime build() {
            return new ElapsedTime(start, duration);
        }
    }
}
