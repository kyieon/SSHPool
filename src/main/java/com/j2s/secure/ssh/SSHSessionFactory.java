package com.j2s.secure.ssh;

import com.j2s.secure.ssh.ex.SSHSessionNotConnectionException;
import com.j2s.secure.ssh.ex.SSHSessionNotFoundException;

import java.io.IOException;

@SuppressWarnings("unchecked")
public class SSHSessionFactory {
	
	public static <T extends SSHSession> T openSession(String sessionKey, String host, String id, String pwd, Class<T> clz) throws Exception {
		return openSession(sessionKey, host, 22, id, pwd, clz);
	}

	public static <T extends SSHSession> T openSession(String sessionKey, String host, int port, String id, String pwd, Class<T> clz) throws Exception {
		if (clz.equals(SSHAsyncSession.class)) {
			return (T) openAsyncSession(sessionKey, host, port, id, pwd);
		}
		return (T) openSyncSession(sessionKey, host, port, id, pwd);
	}

	public static SSHSyncSession openSyncSession(String sessionKey, String host, String id, String pwd) throws Exception {
		return openSyncSession(sessionKey, host, 22, id, pwd);
	}
	
	public static SSHSyncSession openSyncSession(String sessionKey, String host, int port, String id, String pwd) throws Exception {
		SSHSyncSession sshSession = null;
		try {
			sshSession = new SSHSyncSessionImpl(sessionKey);
			sshSession.connect(host, port, id, pwd);
			SSHSessionManager.INSTANCE.putSession(sessionKey, sshSession);
			return sshSession;
		} catch (Exception e) {
			if(null != sshSession) {
				sshSession.close();
			}
			throw e;
		}
	}
	
	/**
	 * @param sessionKey	- Session Key
	 * @param tHost			- Tunnel Server host
	 * @param tPort			- Tunnel Server port
	 * @param tId			- Tunnel Server Id
	 * @param tPwd			- Tunnel server Pwd
	 * @param host			- Server host
	 * @param port			- Server Port
	 * @param id			- Server Id
	 * @param pwd			- Server Pwd
	 */
	public static SSHSyncSession openSyncSessionTunnel(String sessionKey, String tHost, int tPort, String tId, String tPwd, String host, int port, String id, String pwd) throws Exception {
		SSHSyncSession sshSession = null;
		try {
			sshSession = new SSHSyncSessionImpl(sessionKey);
			sshSession.connectTunnel(tHost, tPort, tId, tPwd, host, port, id, pwd);
			SSHSessionManager.INSTANCE.putSession(sessionKey, sshSession);
			return sshSession;
		} catch (Exception e) {
			if(null != sshSession) {
				sshSession.close();
			}
			throw e;
		}
	}

	public static SSHAsyncSession openAsyncSession(String sessionKey, String host, String id, String pwd) throws Exception {
		return openAsyncSession(sessionKey, host, 22, id, pwd);
	}
	
	public static SSHAsyncSession openAsyncSession(String sessionKey, String host, int port, String id, String pwd) throws Exception {
		SSHAsyncSession sshSession = null;
		try {
			sshSession = new SSHAsyncSessionImpl(sessionKey);
			sshSession.connect(host, port, id, pwd);
			SSHSessionManager.INSTANCE.putSession(sessionKey, sshSession);
			return sshSession;
		} catch (Exception e) {
			if(null != sshSession) {
				sshSession.close();
			}
			throw e;
		}
	}
	
	/**
	 * @param sessionKey	- Session Key
	 * @param tHost			- Tunnel Server host
	 * @param tPort			- Tunnel Server port
	 * @param tId			- Tunnel Server Id
	 * @param tPwd			- Tunnel server Pwd
	 * @param host			- Server host
	 * @param port			- Server Port
	 * @param id			- Server Id
	 * @param pwd			- Server Pwd
	 */
	public static SSHAsyncSession openAsyncSessionTunnel(String sessionKey, String tHost, int tPort, String tId, String tPwd, String host, int port, String id, String pwd) throws Exception {
		SSHAsyncSession sshSession = null;
		try {
			sshSession = new SSHAsyncSessionImpl(sessionKey);
			sshSession.connectTunnel(tHost, tPort, tId, tPwd, host, port, id, pwd);
			SSHSessionManager.INSTANCE.putSession(sessionKey, sshSession);
			return sshSession;
		} catch (Exception e) {
			if(null != sshSession) {
				sshSession.close();
			}
			throw e;
		}
	}
	
	public static <T extends SSHSession> T getSession(String sessionKey) throws IOException, SSHSessionNotFoundException, SSHSessionNotConnectionException {
		return (T) SSHSessionManager.INSTANCE.getSession(sessionKey);
	}
}
