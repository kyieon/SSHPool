package com.j2s.secure.ssh;


import com.j2s.secure.SSHSessionConfig;
import com.j2s.secure.SimpleThreadFactory;
import com.j2s.secure.ssh.ex.SSHSessionNotFoundException;
import com.j2s.secure.ssh.ex.SSHSessionNotValidException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.DefaultPooledObjectInfo;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j(topic = "ssh")
public class SSHSyncSessionPool extends GenericObjectPool<SSHSyncSession> {

    private final ScheduledExecutorService ses = Executors.newScheduledThreadPool(1, new SimpleThreadFactory(("SSHSyncSessionPool")));

    public SSHSyncSessionPool(SSHSessionConfig sshSessionConfig) {
        this(sshSessionConfig, getDefaultPoolConfig());
    }

    public SSHSyncSessionPool(SSHSessionConfig sshSessionConfig, GenericObjectPoolConfig<SSHSyncSession> poolConfig) {
        this(new SSHSyncPoolableObjectFactory(sshSessionConfig), poolConfig);
    }

    public SSHSyncSessionPool(SSHSyncPoolableObjectFactory sessionPoolFactory, GenericObjectPoolConfig<SSHSyncSession> poolConfig) {
        super(sessionPoolFactory, poolConfig);
        ses.scheduleAtFixedRate(() -> log.info(toDebugString()), 10, 60, TimeUnit.SECONDS);
    }

    static GenericObjectPoolConfig<SSHSyncSession> getDefaultPoolConfig() {
        GenericObjectPoolConfig<SSHSyncSession> config = new GenericObjectPoolConfig<>();

        config.setMinIdle(4);
        config.setMaxIdle(4);
        config.setMaxTotal(4);
        config.setMaxWait(Duration.ofMillis(30 * 1000L));                        // session wait (borrow timeout)
        config.setTestOnBorrow(true);                                            // brow validation
        config.setTestWhileIdle(true);                                           // idle validation
        config.setTimeBetweenEvictionRuns(Duration.ofMillis(30 * 60 * 1000L));   // check interval time (evictor)
        config.setMinEvictableIdleTime(Duration.ofMillis(60 * 60 * 1000L));      // check idle time
        config.setNumTestsPerEvictionRun(2);
        config.setLifo(false);

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
                log.error("[execute] [" + session.getSessionKey() + "] error.", e);
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
            log.error("[executeOnce] [" + session.getSessionKey() + "] error.", e);
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

    private String currentPoolStatus() {
        return String.format("[Active:%d / Idle:%d]", getNumActive(), getNumIdle());
    }

    private SSHSyncSession getSession() throws Exception {
        try {
            SSHSyncSession session = borrowObject();
            if (null == session) {
                throw new SSHSessionNotFoundException("ssh session not found.");
            }
            return session;
        } catch (NoSuchElementException e) {
            throw new SSHSessionNotValidException(e.getMessage());
        }
    }

    private void releaseSession(SSHSyncSession session) throws Exception {
        returnObject(session);
    }

    private void closeSession(SSHSyncSession session) throws Exception {
        invalidateObject(session);
    }

    private String toDebugString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append("==========================================================");
        sb.append("\n").append("Total: ").append(this.getMaxTotal());
        sb.append("\n").append("Active: ").append(this.getNumActive());
        sb.append("\n").append("Idle: ").append(this.getNumIdle());
        sb.append("\n").append("Objects: ");
        Set<DefaultPooledObjectInfo> objects = this.listAllObjects();
        for (DefaultPooledObjectInfo p : objects) {
            sb.append("\n\t").append("\t").append(p.getPooledObjectToString()).append("\t").append(p.getLastBorrowTimeFormatted());
        }
        sb.append("\n").append("==========================================================");
        return sb.toString();
    }

    public void disconnectAll() {
        clear();
        ses.shutdown();
    }
}
