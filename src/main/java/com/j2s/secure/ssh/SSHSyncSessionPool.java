package com.j2s.secure.ssh;


import com.j2s.secure.SSHSessionConfig;
import com.j2s.secure.SimpleThreadFactory;
import com.j2s.secure.ssh.ex.SSHSessionException;
import com.j2s.secure.ssh.ex.SSHSessionNotConnectionException;
import com.j2s.secure.ssh.ex.SSHSessionNotFoundException;
import com.j2s.secure.ssh.ex.SSHSessionNotValidException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool.impl.GenericObjectPool;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j(topic = "ssh")
public class SSHSyncSessionPool {

	private GenericObjectPool _pool = null;

	private final ScheduledExecutorService ses = Executors.newScheduledThreadPool(1,
			new SimpleThreadFactory(("SSHSyncSessionPool")));

	public SSHSyncSessionPool(SSHSessionConfig sshSessionConfig) {
		this(sshSessionConfig, getDefaultPoolConfig());
	}

	public SSHSyncSessionPool(SSHSessionConfig sshSessionConfig, GenericObjectPool.Config poolConfig) {
		this(new SSHSyncPoolableObjectFactory(sshSessionConfig), poolConfig);
	}

	public SSHSyncSessionPool(SSHSyncPoolableObjectFactory sessionPoolFactory, GenericObjectPool.Config poolConfig) {
		this._pool = new GenericObjectPool(sessionPoolFactory, poolConfig);
		ses.scheduleAtFixedRate(() -> {
			StringBuilder sb = new StringBuilder();
			sb.append("\n==========================================================\n");
			sb.append(debugInfo());
			sb.append("\n==========================================================\n");
			log.info(sb.toString());
		}, 10, 60, TimeUnit.SECONDS);
	}

	static GenericObjectPool.Config getDefaultPoolConfig() {
		GenericObjectPool.Config config = new GenericObjectPool.Config();

		config.minIdle = 4;
		config.maxIdle = 4;
		config.maxActive = 4;
		config.maxWait = 30 * 1000L;  // session wait (borrow timeout)
		config.testOnBorrow = true;   // brow validation
		config.timeBetweenEvictionRunsMillis = 30 * 60 * 1000L; // check interval time (evictor)
		config.minEvictableIdleTimeMillis = 60 * 60 * 1000L;    // check idle time
		config.numTestsPerEvictionRun = 2;
		config.lifo = false;

		return config;
	}

	public String execute(Function<SSHSyncSession, String> fn) throws Exception {
		SSHSyncSession session = getSession();
		synchronized (session) {
			try {
				log.info("[execute] [{}] Pre Execute Pool Status : {}", session.getSessionKey(), currentPoolStatus());
				String result = _execute(fn, session);
				log.debug("[execute] [{}] Post Execute Pool Status : {}", session.getSessionKey(), currentPoolStatus());
				return result;
			} catch (Exception e) {
				log.error("[execute] [" + session.getSessionKey() + "] ", e);
				throw e;
			}
		}
	}

	private String _execute(Function<SSHSyncSession, String> fn, SSHSyncSession session) throws Exception {
		try {
			log.debug("[execute] [{}] Execute : {}", session.getSessionKey(), fn);
			String result = fn.apply(session);
			log.debug("[execute] [{}] Result : {}", session.getSessionKey(), result);
			return result;
		} finally {
			try {
				releaseSession(session);
			} catch (Exception e) {
				log.error("[_execute] [" + session.getSessionKey() + "] release session error", e);
				closeSession(session);
			}
		}
	}

	public String executeOnce(Function<SSHSyncSession, String> fn) throws Exception {
		SSHSyncSession session = getSession();
		try {
			synchronized (session) {
				log.info("[executeOnce] Pre Execute Pool Status : " + currentPoolStatus());
				String result = _executeOnce(fn, session);
				log.debug("[executeOnce] Post Execute Pool Status : " + currentPoolStatus());
				return result;
			}
		} catch (Exception e) {
			log.error("[executeOnce] [" + session.getSessionKey() + "] ", e);
			throw e;
		}
	}

	private String _executeOnce(Function<SSHSyncSession, String> fn, SSHSyncSession session) throws Exception {
		try {
			log.debug("[executeOnce] [" + session.getSessionKey() + "] Execute : {}", fn);
			String result = fn.apply(session);
			log.debug("[executeOnce] [" + session.getSessionKey() + "] Result : {}", result);
			return result;
		} finally {
			closeSession(session);
		}
	}

	private String currentPoolStatus() throws SSHSessionNotConnectionException {
		return String.format("[Active:%d / Idle:%d]", getPool().getNumActive(), getPool().getNumIdle());
	}

	private SSHSyncSession getSession() throws Exception {
		try {
			Object obj = getPool().borrowObject();
			if (null == obj) {
				throw new SSHSessionNotFoundException("ssh session not found.");
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

	private void releaseSession(SSHSyncSession session) throws Exception {
		getPool().returnObject(session);
	}

	private void closeSession(SSHSyncSession session) throws Exception {
		getPool().invalidateObject(session);
	}

	private GenericObjectPool getPool() throws SSHSessionNotConnectionException {
		if (null == _pool) {
			throw new SSHSessionNotConnectionException("ssh session pool was not created.");
		}
		return _pool;
	}

	public String debugInfo() {
		try {
			Method method = GenericObjectPool.class.getDeclaredMethod("debugInfo");
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
