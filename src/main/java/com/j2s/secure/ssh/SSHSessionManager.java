package com.j2s.secure.ssh;

import com.j2s.secure.SessionTimer;
import com.j2s.secure.ssh.ex.SSHSessionAlreadyExistException;
import com.j2s.secure.ssh.ex.SSHSessionNotConnectionException;
import com.j2s.secure.ssh.ex.SSHSessionNotFoundException;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

@Slf4j(topic = "ssh")
enum SSHSessionManager {
    INSTANCE;

    /*
     * Instance field, not a static one. Enum constants are initialized before the class's static
     * field initializers run, so while the enum constructor below executes a static sessionMap
     * would still be null - the monitoring task and the expiration listener registered here could
     * then observe an uninitialized map. As an instance field its initializer runs during the
     * construction of INSTANCE, before the constructor body, so the map is always published
     * before anything can read it.
     */
    private final Map<String, SSHSession> sessionMap = ExpiringMap.builder()
            .expirationPolicy(ExpirationPolicy.ACCESSED)
            .variableExpiration()
            .expiration(10, TimeUnit.MINUTES)
            .asyncExpirationListener((k, v) -> onExpiration(k, (SSHSession) v))
            .build();

    /*
     * Called by ExpiringMap when a session expires. Expiration is dispatched asynchronously, so
     * the monitor acquired here is the same one getSession() holds: an expiring session can no
     * longer be closed while it is concurrently being handed out to a caller.
     */
    private void onExpiration(Object sessionKey, SSHSession session) {
        log.info(sessionKey + " expire session.");
        synchronized (sessionMap) {
            _close(session);
        }
    }

    SSHSessionManager() {
        SessionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                StringBuilder sb = new StringBuilder();
                sb.append("\n==========================================================\n");

                Map<String, SSHSession> sessionMap = SSHSessionManager.INSTANCE.getSessionMap();

                sb.append(String.format("SSH SESSION COUNT [%d] \n", sessionMap.size()));

                sb.append("\nSSH SESSION DETAIL");

                for (Map.Entry<String, SSHSession> e : sessionMap.entrySet()) {
                    SSHSession session = e.getValue();
                    sb.append("\n\t").append(String.format("%s(%s) = [%s][%s][CREATE:%s]]", session.getClass().getName(), session.getName(), session.getSessionKey(), session.isConnected(), session.getCreateDate()));
                }
                sb.append("\n==========================================================\n");

                log.info(sb.toString());
            }
        }, 10, 60);
    }

    public boolean isSession(String sessionKey) {
        return sessionMap.containsKey(sessionKey);
    }

    public void putSession(String sessionKey, SSHSession sshSession) throws SSHSessionAlreadyExistException {
        if (isSession(sessionKey)) {
            throw new SSHSessionAlreadyExistException("'" + sessionKey + "' session key is exist.");
        }
        sessionMap.put(sessionKey, sshSession);
    }

    /*
     * Lookup, connection check and hand-out are atomic with respect to expiration: the async
     * expiration listener acquires the same monitor before closing a session, so a session can no
     * longer expire between the isConnected() check and the caller receiving it.
     */
    public SSHSession getSession(String sessionKey) throws SSHSessionNotFoundException, SSHSessionNotConnectionException {
        synchronized (sessionMap) {
            SSHSession sshSession = sessionMap.get(sessionKey);
            if (null == sshSession) {
                throw new SSHSessionNotFoundException("'" + sessionKey + "' session is not found.");
            }
            if (!sshSession.isConnected()) {
                close(sessionKey);
                throw new SSHSessionNotConnectionException("'" + sessionKey + "' session not connection.");
            }
            return sshSession;
        }
    }

    public void removeSession(String sessionKey) {
        _removeSession(sessionKey);
    }

    private SSHSession _removeSession(String sessionKey) {
        if (!isSession(sessionKey)) return null;

        return sessionMap.remove(sessionKey);
    }

    public Map<String, SSHSession> getSessionMap() {
        return Collections.unmodifiableMap(sessionMap);
    }

    void close(String sessionKey) {
        SSHSession sshSession = _removeSession(sessionKey);
        _close(sshSession);
    }

    void _close(SSHSession sshSession) {
        try {
            if (null != sshSession) sshSession.close();
        } catch (IOException e) {
            log.error("", e);
        }
    }
}
