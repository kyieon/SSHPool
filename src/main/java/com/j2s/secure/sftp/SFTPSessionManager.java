package com.j2s.secure.sftp;

import com.j2s.secure.SessionTimer;
import com.j2s.secure.sftp.ex.SFTPSessionAlreadyExistException;
import com.j2s.secure.sftp.ex.SFTPSessionNotConnectionException;
import com.j2s.secure.sftp.ex.SFTPSessionNotFoundException;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

@Slf4j(topic = "sftp")
enum SFTPSessionManager {
    INSTANCE;

    /*
     * Instance field, not a static one. Enum constants are initialized before the class's static
     * field initializers run, so while the enum constructor below executes a static sessionMap
     * would still be null - the monitoring task and the expiration listener registered here could
     * then observe an uninitialized map. As an instance field its initializer runs during the
     * construction of INSTANCE, before the constructor body, so the map is always published
     * before anything can read it.
     */
    private final Map<String, SFTPSession> sessionMap = ExpiringMap.builder()
            .expirationPolicy(ExpirationPolicy.ACCESSED)
            .variableExpiration()
            .expiration(10, TimeUnit.MINUTES)
            .asyncExpirationListener((k, v) -> onExpiration(k, (SFTPSession) v))
            .build();

    /*
     * Called by ExpiringMap when a session expires. Expiration is dispatched asynchronously, so
     * the monitor acquired here is the same one getSession() holds: an expiring session can no
     * longer be closed while it is concurrently being handed out to a caller.
     */
    private void onExpiration(Object sessionKey, SFTPSession session) {
        log.info(sessionKey + " expire session.");
        synchronized (sessionMap) {
            _close(session);
        }
    }

    SFTPSessionManager() {
        SessionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                StringBuilder sb = new StringBuilder();
                sb.append("\n==========================================================\n");

                Map<String, SFTPSession> sessionMap = SFTPSessionManager.INSTANCE.getSessionMap();

                sb.append(String.format("SFTP SESSION COUNT [%d] \n", sessionMap.size()));

                sb.append("\nSFTP SESSION DETAIL\n");

                for (Map.Entry<String, SFTPSession> e : sessionMap.entrySet()) {
                    SFTPSession session = e.getValue();
                    sb.append(String.format("\tSESSION KEY = [%s][%s][CREATE:%s]]", session.getSessionKey(), session.isConnected(), session.getCreateDate()));
                }
                sb.append("\n==========================================================\n");

                log.info(sb.toString());
            }
        }, 10, 60);
    }

    public boolean isSession(String sessionKey) {
        return sessionMap.containsKey(sessionKey);
    }

    void putSession(String sessionKey, SFTPSession sshSession) throws SFTPSessionAlreadyExistException {
        if (isSession(sessionKey)) {
            throw new SFTPSessionAlreadyExistException("'" + sessionKey + "' session key is exist.");
        }
        sessionMap.put(sessionKey, sshSession);
    }

    /*
     * Lookup, connection check and hand-out are atomic with respect to expiration: the async
     * expiration listener acquires the same monitor before closing a session, so a session can no
     * longer expire between the isConnected() check and the caller receiving it.
     */
    public SFTPSession getSession(String sessionKey) throws SFTPSessionNotFoundException, SFTPSessionNotConnectionException {
        synchronized (sessionMap) {
            SFTPSession sftpSession = sessionMap.get(sessionKey);
            if (null == sftpSession) {
                throw new SFTPSessionNotFoundException("'" + sessionKey + "' session is not found.");
            }
            if (!sftpSession.isConnected()) {
                close(sessionKey);
                throw new SFTPSessionNotConnectionException("'" + sessionKey + "' session not connection.");
            }
            return sftpSession;
        }
    }

    void removeSession(String sessionKey) {
        _removeSession(sessionKey);
    }

    private SFTPSession _removeSession(String sessionKey) {
        if (!isSession(sessionKey)) return null;

        return sessionMap.remove(sessionKey);
    }

    public Map<String, SFTPSession> getSessionMap() {
        return Collections.unmodifiableMap(sessionMap);
    }

    void close(String sessionKey) {
        SFTPSession sftpSession = _removeSession(sessionKey);
        _close(sftpSession);
    }

    void _close(SFTPSession sftpSession) {
        try {
            if (null != sftpSession) sftpSession.close();
        } catch (IOException e) {
            //nothing...
        }
    }
}
