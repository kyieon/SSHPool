package com.j2s.secure.ssh;


import com.j2s.secure.SSHSessionKeyedConfig;
import com.j2s.secure.executors.SecureExecutors;
import com.j2s.secure.ssh.ex.SSHSessionNotFoundException;
import com.j2s.secure.ssh.ex.SSHSessionNotValidException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.DefaultPooledObjectInfo;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j(topic = "ssh")
public class SSHSyncSessionKeyedPool extends GenericKeyedObjectPool<String, SSHSyncSession> {

    private final ScheduledExecutorService ses = SecureExecutors.newScheduledThreadPool(1, "SSHSyncSessionKeyedPool");

    public SSHSyncSessionKeyedPool(SSHSessionKeyedConfig sshSessionConfig) {
        this(sshSessionConfig, getDefaultPoolConfig());
    }

    public SSHSyncSessionKeyedPool(SSHSessionKeyedConfig sshSessionConfig, GenericKeyedObjectPoolConfig<SSHSyncSession> poolConfig) {
        this(new SSHSyncKeyedPoolableObjectFactory(sshSessionConfig), poolConfig);
    }

    public SSHSyncSessionKeyedPool(SSHSyncKeyedPoolableObjectFactory sessionKeyedPoolFactory, GenericKeyedObjectPoolConfig<SSHSyncSession> poolConfig) {
        super(sessionKeyedPoolFactory, poolConfig);
        ses.scheduleAtFixedRate(() -> log.info(toDebugString()), 10, 60, TimeUnit.SECONDS);
    }

    static GenericKeyedObjectPoolConfig<SSHSyncSession> getDefaultPoolConfig() {
        GenericKeyedObjectPoolConfig<SSHSyncSession> config = new GenericKeyedObjectPoolConfig<>();

        config.setMinIdlePerKey(2);
        config.setMaxIdlePerKey(2);
        config.setMaxTotalPerKey(2);
        config.setMaxWait(Duration.ofMillis(30 * 1000L));                            // session wait (borrow timeout)
        config.setTestOnBorrow(true);                                                // brow validation
        config.setTestWhileIdle(true);                                               // idle validation
        config.setTimeBetweenEvictionRuns(Duration.ofMillis(30 * 60 * 1000L));       // check interval time (evictor)
        config.setMinEvictableIdleTime(Duration.ofMillis(-1L));                      // check idle time
        config.setNumTestsPerEvictionRun(2);
        config.setLifo(false);

        return config;
    }

    public String execute(String host, Function<SSHSyncSession, String> fn) throws Exception {
        SSHSyncSession session = getSession(host);
        synchronized (session) {
            try {
                log.info("[execute] [{}][{}] Pre Execute Pool Status : {}", session.getSessionKey(), host, currentPoolStatus());
                String result = _execute(host, fn, session);
                log.debug("[execute] [{}][{}] Post Execute Pool Status : {}", session.getSessionKey(), host, currentPoolStatus());
                return result;
            } catch (Exception e) {
                log.error("[execute] [" + session.getSessionKey() + "][" + host + "] error.", e);
                throw e;
            }
        }
    }

    private String _execute(String host, Function<SSHSyncSession, String> fn, SSHSyncSession session) throws Exception {
        try {
            log.debug("[_execute] [{}][{}] Execute : {}", session.getSessionKey(), host, fn);
            String result = fn.apply(session);
            log.debug("[_execute] [{}][{}] Result : {}", session.getSessionKey(), host, result);
            return result;
        } finally {
            try {
                releaseSession(host, session);
            } catch (Exception e) {
                log.error("[_execute] [" + session.getSessionKey() + "][" + host + "] release session error.", e);
                closeSession(host, session);
            }
        }
    }

    private String currentPoolStatus() {
        return String.format("[Active:%d / Idle:%d]", getNumActive(), getNumIdle());
    }

    private SSHSyncSession getSession(String host) throws Exception {
        try {
            SSHSyncSession session = borrowObject(host);
            if (null == session) {
                throw new SSHSessionNotFoundException(String.format("[%s] ssh session not found.", host));
            }
            return session;
        } catch (NoSuchElementException e) {
            throw new SSHSessionNotValidException(e.getMessage());
        }
    }

    private void releaseSession(String host, SSHSyncSession session) throws Exception {
        returnObject(host, session);
    }

    private void closeSession(String host, SSHSyncSession session) throws Exception {
        invalidateObject(host, session);
    }

    private String toDebugString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append("==========================================================");
        sb.append("\n").append("TotalPerKey: ").append(this.getMaxTotalPerKey());
        sb.append("\n").append("Active: ").append(this.getNumActive());
        sb.append("\n").append("Idle: ").append(this.getNumIdle());
        sb.append("\n").append("Objects: ");
        Map<String, List<DefaultPooledObjectInfo>> objects = this.listAllObjects();
        for (String key : objects.keySet()) {
            for (DefaultPooledObjectInfo p : objects.get(key)) {
                sb.append("\n\t").append(key).append("\t").append(p.getPooledObjectToString()).append("\t").append(p.getLastBorrowTimeFormatted()).append("("+p.getBorrowedCount()+")");
            }
        }
        sb.append("\n").append("==========================================================");
        return sb.toString();
    }

    public void disconnectAll() {
        clear();
        ses.shutdown();
    }
}
