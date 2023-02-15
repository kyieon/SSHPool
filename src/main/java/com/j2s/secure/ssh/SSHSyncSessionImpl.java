package com.j2s.secure.ssh;

import com.j2s.secure.SimpleThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j(topic = "ssh")
class SSHSyncSessionImpl extends SSHAbstractSession implements SSHSyncSession {

	private final int DEFAULT_TIMEOUT = 60;

	private ExecutorService readES = Executors.newFixedThreadPool(1, new SimpleThreadFactory("SSHSyncSessionReader"));

	private AtomicLong errorCount = new AtomicLong();

	private final Lock l = new ReentrantLock();

	public SSHSyncSessionImpl(String sessionKey) {
		super(sessionKey);
	}

	@Override
	public void writeVoid(String command) {
		try {
			this.write(command, null, DEFAULT_TIMEOUT);
		} catch (Exception e) {
			log.error("", "");
		}
	}

	@Override
	public String write(String command) throws IOException {
		return this.write(command, null, DEFAULT_TIMEOUT);
	}

	@Override
	public String write(String command, int timeOut) throws IOException {
		return this.write(command, null, timeOut);
	}

	@Override
	public String write(String command, String prompt) throws IOException {
		return this.write(command, prompt, DEFAULT_TIMEOUT);
	}

	@Override
	public String write(String command, String prompt, int timeOut) throws IOException {
		l.lock();
		try {
			log.debug("[" + getSessionKey() + "][" + toString() + "] WRITE START :: COMMAND :: " + command);
			_write(command);
			Future<String> f = readES.submit(() -> read(prompt));
			try {
				String result = f.get(timeOut, TimeUnit.SECONDS);
				log.trace("[" + getSessionKey() + "][" + toString() + "] COMMAND :: {} :: RESULT :: {}", command, result);
				log.debug("[" + getSessionKey() + "][" + toString() + "] WRITE END   :: COMMAND :: {}", command);
				return result;
			} catch (TimeoutException | InterruptedException | ExecutionException e) {
				_write(CTRL_C);
				f.cancel(true);
				throw e;
			}
		} catch (IOException e) {
			errorCount.incrementAndGet();
			log.error("[" + getSessionKey() + "] [" + displayPoolInfo() + "] WRITE ERROR :: COMMAND :: " + command, e);
			throw e;
		} catch (TimeoutException | InterruptedException | ExecutionException e) {
			errorCount.incrementAndGet();
			log.error("[" + getSessionKey() + "] [" + displayPoolInfo() + "] WRITE ERROR :: COMMAND :: " + command
					+ " :: timeOut :: " + timeOut, e);
			throw new IOException("[" + getSessionKey() + "] COMMAND :: " + command, e);
		} catch (Exception e) {
			errorCount.incrementAndGet();
			log.error("[" + getSessionKey() + "] [" + displayPoolInfo() + "] WRITE ERROR :: COMMAND :: " + command, e);
			throw new IOException("[" + getSessionKey() + "] COMMAND :: " + command, e);
		} finally {
			l.unlock();
		}
	}

	private String displayPoolInfo() {
		ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) this.readES;
		int activeCount = threadPoolExecutor.getActiveCount();
		long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
		long totalCount = threadPoolExecutor.getTaskCount();
		return getQueueCount() + "/" + (completedTaskCount + activeCount) + "/" + totalCount + "/" + getErrorCount();
	}

	@Override
	public long getErrorCount() {
		return errorCount.get();
	}

	@Override
	public long getQueueCount() {
		ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) this.readES;
		return threadPoolExecutor.getQueue().size();
	}

	private void _write(String command) throws IOException {
		os.write(command.getBytes());
		os.write("\n".getBytes());
		os.flush();
	}

	@Override
	protected String read(String prompt) {
		StringBuilder sb = new StringBuilder();
		boolean stop = false;
		byte[] b = new byte[1024 * 4];
		try {
			while (true) {
				while (is.available() > 0) {
					int i = is.read(b);
					if (i < 0) {
						break;
					}
					String result = new String(b, 0, i);
					log.trace("[" + getSessionKey() + "]" + "[" + i + "]" + " read :: " + result);
					sb.append(result);
					if (null == prompt) {
						if (isPromptMet(result)) {
							stop = true;
							break;
						}
					} else {
						if (sb.toString().trim().contains(prompt)) {
							stop = true;
							break;
						}
					}

					if (!channel.isConnected()) {
						stop = true;
						break;
					}
				}
				if (stop) {
					break;
				}
				Thread.sleep(100L);
			}
		} catch (Exception e) {
			// nothing...
		}
		return sb.toString();
	}

	private boolean isPromptMet(String result) {
		if (result == null) {
			return true;
		}
		String[] end_prompts = { "$", "#", "(y/n)", "(yes/no)?", "password:", "Password:", "[yes,no]",
				"[yes/no/CANCEL]", "[y/n]?", ">):", "(N/Y):", "(Y/N):", "2004h", "\u001B[6n" };

		for (String end_prompt : end_prompts) {
			if (result.trim().endsWith(end_prompt)) {
				return true;
			}
		}

		if (isErrorResultMet(result)) {
			return true;
		}
		return false;
	}

	private static boolean isErrorResultMet(String result) {
		String[] errorResults = { "Connection to COM failed", "Maximum number of administrators has been reached",
				"Initialization is not complete", };
		for (String errorResult : errorResults) {
			if (result.contains(errorResult)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void close() throws IOException {
		readES.shutdown();
		super.close();
	}
}
