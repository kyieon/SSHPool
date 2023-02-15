package com.j2s.secure.ssh;


import com.j2s.secure.SSHSessionConfig;
import com.j2s.secure.SimpleThreadFactory;
import com.j2s.secure.ssh.ex.SSHSessionException;
import com.j2s.secure.ssh.ex.SSHSessionNotConnectionException;
import com.j2s.secure.ssh.ex.SSHSessionNotFoundException;
import com.j2s.secure.ssh.ex.SSHSessionNotValidException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j(topic = "ssh")
public class SSHSyncSessionKeyedPool {

	private GenericKeyedObjectPool _pool = null;

	private final ScheduledExecutorService ses = Executors.newScheduledThreadPool(1,
			new SimpleThreadFactory(("SSHSyncSessionKeyedPool")));

	public SSHSyncSessionKeyedPool(SSHSessionConfig sshSessionConfig) {
		this(sshSessionConfig, getDefaultPoolConfig());
	}

	public SSHSyncSessionKeyedPool(SSHSessionConfig sshSessionConfig, GenericKeyedObjectPool.Config poolConfig) {
		this(new SSHSyncKeyedPoolableObjectFactory(sshSessionConfig), poolConfig);
	}

	public SSHSyncSessionKeyedPool(SSHSyncKeyedPoolableObjectFactory sessionKeyedPoolFactory,
			GenericKeyedObjectPool.Config poolConfig) {
		this._pool = new GenericKeyedObjectPool(sessionKeyedPoolFactory, poolConfig);
		ses.scheduleAtFixedRate(() -> {
			StringBuilder sb = new StringBuilder();
			sb.append("\n==========================================================\n");
			sb.append(debugInfo());
			sb.append("\n==========================================================\n");
			log.info(sb.toString());
		}, 10, 60, TimeUnit.SECONDS);
	}

	static GenericKeyedObjectPool.Config getDefaultPoolConfig() {
		GenericKeyedObjectPool.Config config = new GenericKeyedObjectPool.Config();

		config.minIdle = 2;
		config.maxIdle = 2;
		config.maxActive = 2;
		config.maxWait = 30 * 1000L;  // session wait (borrow timeout)
		config.testOnBorrow = true;   // brow validation
		config.timeBetweenEvictionRunsMillis = 30 * 60 * 1000L; // check interval time (evictor)
		config.minEvictableIdleTimeMillis = 60 * 60 * 1000L;    // check idle time
		config.numTestsPerEvictionRun = 1;
		config.lifo = false;

		return config;
	}

	public String execute(String key, Function<SSHSyncSession, String> fn) throws Exception {
		SSHSyncSession session = getSession(key);
		synchronized (session) {
			try {
				log.info("[execute] [{}][{}] Pre Execute Pool Status : {}", key, session.getSessionKey(), currentPoolStatus());
				String result = _execute(key, fn, session);
				log.debug("[execute] [{}][{}] Post Execute Pool Status : {}", key, session.getSessionKey(), currentPoolStatus());
				return result;
			} catch (Exception e) {
				log.error("[execute] [" + key + "][" + session.getSessionKey() + "] ", e);
				throw e;
			}
		}
	}

	private String _execute(String key, Function<SSHSyncSession, String> fn, SSHSyncSession session)
			throws Exception {
		try {
			log.debug("[_execute] [{}][{}] Execute : {}", key, session.getSessionKey(), fn);
			String result = fn.apply(session);
			log.debug("[_execute] [{}][{}] Result : {}", key, session.getSessionKey(), result);
			return result;
		} finally {
			try {
				releaseSession(key, session);
			} catch (Exception e) {
				log.error("[_execute] [" + key + "][" + session.getSessionKey() + "] release session error", e);
				closeSession(key, session);
			}
		}
	}

	private String currentPoolStatus() throws SSHSessionNotConnectionException {
		return String.format("[Active:%d / Idle:%d]", getPool().getNumActive(), getPool().getNumIdle());
	}

	private SSHSyncSession getSession(String key) throws Exception {
		try {
			Object obj = getPool().borrowObject(key);
			if (null == obj) {
				throw new SSHSessionNotFoundException(String.format("[%s] ssh session not found.", key));
			}
			return convertSessionObj(obj);
		} catch (NoSuchElementException e) {
			throw new SSHSessionNotValidException(e.getMessage());
		}
	}

	private SSHSyncSession convertSessionObj(Object obj) throws SSHSessionException {
		if (obj instanceof SSHSyncSession) {
			return (SSHSyncSession) obj;
		}
		throw new SSHSessionException("class cast error. " + obj.getClass().getName());
	}

	private void releaseSession(String key, SSHSyncSession session) throws Exception {
		getPool().returnObject(key, session);
	}

	private void closeSession(String key, SSHSyncSession session) throws Exception {
		getPool().invalidateObject(key, session);
	}

	private GenericKeyedObjectPool getPool() throws SSHSessionNotConnectionException {
		if (null == _pool) {
			throw new SSHSessionNotConnectionException("ssh session pool was not created.");
		}
		return _pool;
	}

	public String debugInfo() {
		try {
			Method method = GenericKeyedObjectPool.class.getDeclaredMethod("debugInfo");
			method.setAccessible(true);
			Object result = method.invoke(_pool);
			return result.toString();
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
			// nothing
		}
		return null;
	}

	public void disconnectAll() {
		try {
			getPool().clear();
			ses.shutdown();
		} catch (SSHSessionNotConnectionException e) {
			log.error("", e);
		}
	}
}
