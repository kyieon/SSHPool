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

	private static final Map<String, SFTPSession> sessionMap = ExpiringMap.builder()
			.expirationPolicy(ExpirationPolicy.ACCESSED)
			.variableExpiration()
			.expiration(10, TimeUnit.MINUTES)
			.asyncExpirationListener((k, v) -> { 
				log.info(k + " expire session.");
				SFTPSessionManager.INSTANCE._close((SFTPSession) v);
			})
			.build();

    public boolean isSession(String sessionKey) {
        return sessionMap.containsKey(sessionKey);
    }

    void putSession(String sessionKey, SFTPSession sshSession) throws SFTPSessionAlreadyExistException {
        if (isSession(sessionKey)) {
            throw new SFTPSessionAlreadyExistException("'" + sessionKey + "' session key is exist.");
        }
        sessionMap.put(sessionKey, sshSession);
    }

    public SFTPSession getSession(String sessionKey) throws SFTPSessionNotFoundException, SFTPSessionNotConnectionException {
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
