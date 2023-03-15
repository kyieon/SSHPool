package com.j2s.secure.ssh;

import java.io.IOException;

import com.j2s.secure.ssh.ex.SSHSessionNotConnectionException;
import com.j2s.secure.ssh.ex.SSHSessionNotFoundException;
import com.jcraft.jsch.JSchException;

@SuppressWarnings("unchecked")
public class SSHSessionFactory {
	
	public static <T extends SSHSession> T openSession(String sessionKey, String host, String id, String pwd, Class<T> clz) throws JSchException, IOException {
		return openSession(sessionKey, host, 22, id, pwd, clz);
	}

	public static <T extends SSHSession> T openSession(String sessionKey, String host, int port, String id, String pwd, Class<T> clz) throws JSchException, IOException {
		if (clz.equals(SSHAsyncSession.class)) {
			return (T) openAsyncSession(sessionKey, host, port, id, pwd);
		}
		return (T) openSyncSession(sessionKey, host, port, id, pwd);
	}

	public static SSHSyncSession openSyncSession(String sessionKey, String host, String id, String pwd) throws JSchException, IOException {
		return openSyncSession(sessionKey, host, 22, id, pwd);
	}
	
	public static SSHSyncSession openSyncSession(String sessionKey, String host, int port, String id, String pwd) throws JSchException, IOException {
		SSHSyncSession sshSession = new SSHSyncSessionImpl(sessionKey);
		sshSession.connect(host, port, id, pwd);
		SSHSessionManager.INSTANCE.putSession(sessionKey, sshSession);
		return sshSession;
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
	public static SSHSyncSession openSyncSessionTunnel(String sessionKey, String tHost, int tPort, String tId, String tPwd, String host, int port, String id, String pwd) throws JSchException, IOException {
		SSHSyncSession sshSession = new SSHSyncSessionImpl(sessionKey);
		sshSession.connectTunnel(tHost, tPort, tId, tPwd, host, port, id, pwd);
		SSHSessionManager.INSTANCE.putSession(sessionKey, sshSession);
		return sshSession;
	}

	public static SSHAsyncSession openAsyncSession(String sessionKey, String host, String id, String pwd) throws JSchException, IOException {
		return openAsyncSession(sessionKey, host, 22, id, pwd);
	}
	
	public static SSHAsyncSession openAsyncSession(String sessionKey, String host, int port, String id, String pwd) throws JSchException, IOException {
		SSHAsyncSession sshSession = new SSHAsyncSessionImpl(sessionKey);
		sshSession.connect(host, port, id, pwd);
		SSHSessionManager.INSTANCE.putSession(sessionKey, sshSession);
		return sshSession;
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
	public static SSHAsyncSession openAsyncSessionTunnel(String sessionKey, String tHost, int tPort, String tId, String tPwd, String host, int port, String id, String pwd) throws JSchException, IOException {
		SSHAsyncSession sshSession = new SSHAsyncSessionImpl(sessionKey);
		sshSession.connectTunnel(tHost, tPort, tId, tPwd, host, port, id, pwd);
		SSHSessionManager.INSTANCE.putSession(sessionKey, sshSession);
		return sshSession;
	}
	
	public static <T extends SSHSession> T getSession(String sessionKey) throws IOException, SSHSessionNotFoundException, SSHSessionNotConnectionException {
		return (T) SSHSessionManager.INSTANCE.getSession(sessionKey);
	}
}
