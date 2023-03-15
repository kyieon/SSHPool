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

    SSHSessionManager() {
        SessionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                StringBuilder sb = new StringBuilder();
                sb.append("\n==========================================================\n");

                Map<String, SSHSession> sessionMap = SSHSessionManager.INSTANCE.getSessionMap();

                sb.append(String.format("SSH SESSION COUNT [%d] \n", sessionMap.size()));

                sb.append("\nSSH SESSION DETAIL\n");

                for (Map.Entry<String, SSHSession> e : sessionMap.entrySet()) {
                    SSHSession session = e.getValue();
                    sb.append(String.format("\tSESSION KEY = [%s][%s][CREATE:%s]]", session.getSessionKey(), session.isConnected(), session.getCreateDate()));
                }
                sb.append("\n==========================================================\n");

                log.info(sb.toString());
            }
        }, 10, 60);
    }

	private static final Map<String, SSHSession> sessionMap = ExpiringMap.builder()
			.expirationPolicy(ExpirationPolicy.ACCESSED)
			.variableExpiration()
			.expiration(10, TimeUnit.MINUTES)
			.asyncExpirationListener((k, v) -> {
				log.info(k + " expire session.");
				SSHSessionManager.INSTANCE._close((SSHSession) v);
			})
			.build();

    public boolean isSession(String sessionKey) {
        return sessionMap.containsKey(sessionKey);
    }

    public void putSession(String sessionKey, SSHSession sshSession) throws SSHSessionAlreadyExistException {
        if (isSession(sessionKey)) {
            throw new SSHSessionAlreadyExistException("'" + sessionKey + "' session key is exist.");
        }
        sessionMap.put(sessionKey, sshSession);
    }

    public SSHSession getSession(String sessionKey) throws SSHSessionNotFoundException, SSHSessionNotConnectionException {
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
            // nothing...
        }
    }
}
