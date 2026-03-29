package jo.jung.clientmodule.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ReactiveRateLimiter {

    private final int permitsPerSecond;
    private final Queue<Sinks.One<Void>> waitingQueue = new ConcurrentLinkedQueue<>();

    public ReactiveRateLimiter(int permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;

        // 초당 permitsPerSecond 만큼 토큰 방출
        Flux.interval(Duration.ofMillis(1000L / permitsPerSecond))
                .onBackpressureDrop()
                .publishOn(Schedulers.parallel())
                .subscribe(tick -> {
                    Sinks.One<Void> sink = waitingQueue.poll();
                    if (sink != null) {
                        sink.tryEmitEmpty();
                    }
                });
    }

    public Mono<Void> acquire() {
        Sinks.One<Void> sink = Sinks.one();
        waitingQueue.add(sink);
        return sink.asMono();
    }
}
