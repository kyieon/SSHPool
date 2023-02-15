package com.j2s.secure.ssh;

import com.j2s.secure.SSHAsyncMessage;
import com.j2s.secure.SimpleThreadFactory;
import com.j2s.secure.ssh.ex.SSHTriggerAlreadyExistException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j(topic = "ssh")
class SSHAsyncSessionImpl extends SSHAbstractSession implements SSHAsyncSession {

	private ExecutorService readES = Executors.newFixedThreadPool(1, new SimpleThreadFactory("SSHAsyncSessionReader"));
	private ExecutorService sendES = Executors.newFixedThreadPool(1, new SimpleThreadFactory("SSHAsyncSessionSender"));
	private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

	public SSHAsyncSessionImpl(String sessionKey) {
		super(sessionKey);
	}

	@Override
	protected String read(String prompt) {
		readES.submit(() -> {
			byte[] b = new byte[1024];
			while(true) {
				try {
					if(!channel.isConnected()) {
						break;
					}
					while(is.available() > 0) {
						int i = is.read(b);
						if(i < 0) {
							break;
						}
						String result = new String(b, 0, i);
						messageQueue.put(result);
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
		return null;
	}

	@Override
	public void writeVoid(String command) {
		try {
			write(command);
		} catch (Exception e) {
			log.error("", e);
		}
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
		readES.shutdown();
		sendES.shutdown();
		messageQueue.clear();
		super.close();
	}

	private AtomicBoolean onTriggerStart = new AtomicBoolean(false);

	@Override
	public void onTrigger(Consumer<SSHAsyncMessage> consumer) throws SSHTriggerAlreadyExistException {
		if(onTriggerStart.get()) {
			throw new SSHTriggerAlreadyExistException();
		}

		synchronized (this) {
			sendES.submit(() -> {
				while(true) {
					try {
						String message = messageQueue.poll(100L, TimeUnit.MILLISECONDS);
						if(null == message) {
							continue;
						}
						consumer.accept(new SSHAsyncMessage(sessionKey, message));
					} catch (InterruptedException e) {
						log.warn("[" + Thread.currentThread().getName() + "]" + " InterruptedException.");
						break;
					}
				}
			});
			onTriggerStart.set(true);
		}
	}
}
