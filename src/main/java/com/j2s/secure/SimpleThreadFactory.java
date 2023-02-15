package com.j2s.secure;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class SimpleThreadFactory implements ThreadFactory {
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