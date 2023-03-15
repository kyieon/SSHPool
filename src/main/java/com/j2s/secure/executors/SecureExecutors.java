package com.j2s.secure.executors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class SecureExecutors {

    public static ExecutorService newFixedThreadPool(int nThreads, String prefix) {
        return Executors.newFixedThreadPool(nThreads, new SimpleThreadFactory(prefix));
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, String prefix) {
        return Executors.newScheduledThreadPool(corePoolSize, new SimpleThreadFactory(prefix));
    }

    private static class SimpleThreadFactory implements ThreadFactory {
        private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
        private final String prefix;

        public SimpleThreadFactory(final String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(final Runnable r) {
            final Thread t = defaultThreadFactory.newThread(r);

            if (prefix != null && !prefix.isEmpty()) {
                t.setName(prefix + "-" + t.getName());
            }

            return t;
        }
    }
}
