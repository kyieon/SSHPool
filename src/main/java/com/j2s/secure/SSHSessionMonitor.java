package com.j2s.secure;

import lombok.extern.slf4j.Slf4j;

import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
class SSHSessionTimer {

	private static ScheduledExecutorService ses = null;
	private static AtomicInteger usageCount = new AtomicInteger(0);

	static synchronized void schedule(TimerTask task, long delay, long period) throws Exception {
		if(null == ses) {
			ses = Executors.newScheduledThreadPool(1, new SimpleThreadFactory("SSHSessionMonitor"));
		}
		int count = usageCount.incrementAndGet();
		log.info("{} Start - {}", task.toString(), count);
		ses.scheduleAtFixedRate(task, delay, period, TimeUnit.SECONDS);
	}

	static synchronized void cancel(TimerTask task) {
		task.cancel();
		int count = usageCount.decrementAndGet();
		if (count == 0) {
			ses.shutdown();
			ses = null;
		}
	}
}
