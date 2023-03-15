package com.j2s.secure;

import com.j2s.secure.executors.SecureExecutors;
import lombok.extern.slf4j.Slf4j;

import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class SessionTimer {

	private static ScheduledExecutorService ses = null;
	private static AtomicInteger usageCount = new AtomicInteger(0);

	public static synchronized void schedule(TimerTask task, long delay, long period) {
		if(null == ses) {
			ses = SecureExecutors.newScheduledThreadPool(2, "SSHSessionMonitor");
		}
		try {
			int count = usageCount.incrementAndGet();
			log.info("{} Start - {}", task.toString(), count);
			ses.scheduleAtFixedRate(task, delay, period, TimeUnit.SECONDS);
		} catch (Exception e) {
			log.error("", e);
			cancel(task);
		}
	}

	private static synchronized void cancel(TimerTask task) {
		task.cancel();
		int count = usageCount.decrementAndGet();
		if (count == 0) {
			ses.shutdown();
			ses = null;
		}
	}
}
