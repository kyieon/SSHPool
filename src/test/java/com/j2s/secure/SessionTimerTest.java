package com.j2s.secure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Timeout(30)
class SessionTimerTest {

	private static TimerTask noopTask() {
		return new TimerTask() {
			@Override
			public void run() {
				// monitoring body; never actually fires within a test (1h delay)
			}
		};
	}

	@BeforeEach
	void resetSharedState() throws Exception {
		// SessionTimer keeps JVM-global state; isolate every test from the others.
		setStatic("ses", null);
		((AtomicInteger) getStatic("usageCount")).set(0);
		((Set<?>) getStatic("liveTasks")).clear();
	}

	@Test
	void cancellingAllTasksShutsDownSharedExecutor() {
		TimerTask taskA = noopTask();
		TimerTask taskB = noopTask();

		SessionTimer.schedule(taskA, 3600, 3600);
		SessionTimer.schedule(taskB, 3600, 3600);
		assertNotNull(readSes());

		SessionTimer.cancel(taskA);
		assertNotNull(readSes()); // still one live registration

		SessionTimer.cancel(taskB);
		assertNull(readSes()); // last release -> shutdown + nulled
	}

	@Test
	void repeatedCancelIsIdempotentAndDoesNotUnderflow() {
		TimerTask task = noopTask();
		SessionTimer.schedule(task, 3600, 3600);

		SessionTimer.cancel(task);
		assertNull(readSes());

		// repeat cancel + unknown + null must all be harmless no-ops
		assertDoesNotThrow(() -> SessionTimer.cancel(task));
		assertDoesNotThrow(() -> SessionTimer.cancel(null));
		assertDoesNotThrow(() -> SessionTimer.cancel(noopTask()));

		assertNull(readSes()); // state untouched by the no-ops
	}

	@Test
	void scheduleAgainWorksAfterDoubleCancel() {
		TimerTask task = noopTask();
		SessionTimer.schedule(task, 3600, 3600);
		SessionTimer.cancel(task);
		SessionTimer.cancel(task); // must not underflow usageCount

		TimerTask again = noopTask();
		assertDoesNotThrow(() -> SessionTimer.schedule(again, 3600, 3600));
		assertNotNull(readSes()); // executor recreated cleanly

		SessionTimer.cancel(again);
		assertNull(readSes()); // leaves global state clean
	}

	private static Object readSes() {
		return getStatic("ses");
	}

	private static Object getStatic(String name) {
		try {
			Field f = SessionTimer.class.getDeclaredField(name);
			f.setAccessible(true);
			return f.get(null);
		} catch (Exception e) {
			throw new AssertionError("cannot read static field: " + name, e);
		}
	}

	private static void setStatic(String name, Object value) throws Exception {
		Field f = SessionTimer.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(null, value);
	}
}
