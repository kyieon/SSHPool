package com.j2s.secure.ssh;

import com.j2s.secure.SSHAsyncMessage;
import com.j2s.secure.executors.SecureExecutors;
import com.j2s.secure.ssh.ex.SSHTriggerAlreadyExistException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j(topic = "ssh")
class SSHAsyncSessionImpl extends SSHAbstractSession implements SSHAsyncSession {

	private ExecutorService readES = SecureExecutors.newFixedThreadPool(1, "SSHAsyncSessionReader");
	private ExecutorService sendES = SecureExecutors.newFixedThreadPool(1, "SSHAsyncSessionSender");
	private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

	private final AtomicBoolean readerStarted = new AtomicBoolean(false);
	private final AtomicBoolean onTriggerStart = new AtomicBoolean(false);
	private final AtomicBoolean idleTimeoutScheduled = new AtomicBoolean(false);
	private volatile ScheduledExecutorService idleTimeoutExecutor;

	public SSHAsyncSessionImpl(String sessionKey) {
		super(sessionKey);
	}

	@Override
	protected int getDefaultReadTimeout() {
		return Integer.MAX_VALUE;
	}

	@Override
	protected String read(String prompt, int timeOut) {
		startReader();
		scheduleIdleTimeout(timeOut);
		return null;
	}

	/**
	 * Starts the background reader loop exactly once. Without the guard every read()
	 * call stacks another reader task on the single-thread executor and each of them
	 * pushes duplicate data into the shared message queue.
	 */
	private void startReader() {
		if (!readerStarted.compareAndSet(false, true)) {
			log.debug("[{}] reader already started.", getSessionKey());
			return;
		}
		readES.submit(() -> {
			byte[] b = new byte[1024];
			while (true) {
				try {
					if (!channel.isConnected()) {
						break;
					}
					while (is.available() > 0) {
						int i = is.read(b);
						if (i < 0) {
							break;
						}
						messageQueue.put(new String(b, 0, i));
					}
					Thread.sleep(100L);
				} catch (InterruptedException e) {
					log.error("[" + Thread.currentThread().getName() + "]" + " InterruptedException.");
					break;
				} catch (IOException e) {
					//nothing
				}
			}
		});
	}

	/**
	 * Schedules the idle-timeout close exactly once on a single reusable scheduler
	 * that is shut down by close(). The async default timeout is Integer.MAX_VALUE
	 * (~68 years away) which is treated as "no timeout", so no scheduler thread is
	 * kept alive for nothing.
	 */
	private void scheduleIdleTimeout(int timeOut) {
		if (timeOut <= 0 || timeOut == Integer.MAX_VALUE) {
			return;
		}
		if (!idleTimeoutScheduled.compareAndSet(false, true)) {
			return;
		}
		if (null == idleTimeoutExecutor) {
			idleTimeoutExecutor = Executors.newSingleThreadScheduledExecutor();
		}
		idleTimeoutExecutor.schedule(() -> {
			try {
				log.info("[" + getSessionKey() + "]" + " close schedule");
				close();
			} catch (IOException e) {
				log.error("[" + getSessionKey() + "] scheduled close failed.", e);
			}
		}, timeOut, TimeUnit.SECONDS);
	}

	@Override
	public void write(String command) throws IOException {
		log.debug("[" + getSessionKey() + "] write :: " + command);
		_write(command);
	}

	private void _write(String command) throws IOException {
		os.write(command.getBytes());
		os.write("\n".getBytes());
		os.flush();
	}

	@Override
	public void close() throws IOException {
		ScheduledExecutorService scheduler = idleTimeoutExecutor;
		if (null != scheduler) {
			scheduler.shutdown();
		}
		readES.shutdown();
		sendES.shutdown();
		messageQueue.clear();
		super.close();
	}

	@Override
	public void onTrigger(Consumer<SSHAsyncMessage> consumer) throws SSHTriggerAlreadyExistException {
		synchronized (this) {
			if (!onTriggerStart.compareAndSet(false, true)) {
				throw new SSHTriggerAlreadyExistException();
			}
			sendES.submit(() -> {
				while (true) {
					try {
						String message = messageQueue.poll(100L, TimeUnit.MILLISECONDS);
						if (null == message) {
							continue;
						}
						consumer.accept(new SSHAsyncMessage(sessionKey, message));
					} catch (InterruptedException e) {
						log.warn("[" + Thread.currentThread().getName() + "]" + " InterruptedException.");
						break;
					}
				}
			});
		}
	}
}
