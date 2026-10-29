package io.github.pirut.ccagent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Lua jobs sent to computers, and which computers have the agent hook running. */
public final class Jobs {
    public record Result(boolean ok, String values, String output, String error, long millis) {
    }

    public static final class Job {
        public final int id;
        public final int computerId;
        public final String kind;
        public final long started = System.currentTimeMillis();
        public final CompletableFuture<Result> future = new CompletableFuture<>();

        Job(int id, int computerId, String kind) {
            this.id = id;
            this.computerId = computerId;
            this.kind = kind;
        }
    }

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    private static final Map<Integer, Job> RUNNING = new ConcurrentHashMap<>();
    private static final Set<Integer> READY = ConcurrentHashMap.newKeySet();
    private static final Map<Integer, Job> FINISHED = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Job> eldest) {
            return size() > 200;
        }
    };

    private Jobs() {
    }

    public static Job create(int computerId, String kind) {
        var job = new Job(NEXT_ID.getAndIncrement(), computerId, kind);
        RUNNING.put(job.id, job);
        return job;
    }

    public static Job get(int id) {
        var job = RUNNING.get(id);
        if (job != null) return job;
        synchronized (FINISHED) {
            return FINISHED.get(id);
        }
    }

    public static void complete(int id, Result result) {
        var job = RUNNING.remove(id);
        if (job == null) return;
        synchronized (FINISHED) {
            FINISHED.put(id, job);
        }
        job.future.complete(result);
    }

    public static boolean isReady(int computerId) {
        return READY.contains(computerId);
    }

    static void markReady(int computerId) {
        READY.add(computerId);
    }

    static void computerStarted(int computerId) {
        READY.remove(computerId);
    }

    /** The computer shut down or rebooted: its running jobs can never finish. */
    static void computerStopped(int computerId) {
        READY.remove(computerId);
        for (var job : RUNNING.values()) {
            if (job.computerId == computerId) {
                complete(job.id, new Result(false, null, "", "the computer shut down or rebooted while the job ran",
                    System.currentTimeMillis() - job.started));
            }
        }
    }

    static void clear() {
        for (var job : RUNNING.values()) {
            complete(job.id, new Result(false, null, "", "the world closed", 0));
        }
        READY.clear();
    }
}
