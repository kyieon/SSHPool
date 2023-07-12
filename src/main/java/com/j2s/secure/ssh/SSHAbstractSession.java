package com.j2s.secure.ssh;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Slf4j(topic = "ssh")
abstract class SSHAbstractSession implements SSHSession {

	protected Session session = null;
	protected Session sessionTunnel = null;
	protected Channel channel = null;
	protected InputStream is = null;
	protected OutputStream os = null;
	protected String name;
	protected String sessionKey;
	protected LocalDateTime createTime = LocalDateTime.now();

	protected final static String CTRL_C = String.valueOf((char)3);

	public SSHAbstractSession(String sessionKey) {
		this.sessionKey = sessionKey;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getSessionKey() {
		return sessionKey;
	}

	@Override
	public LocalDateTime getCreateDate() {
		return createTime;
	}

	@Override
	public void connect(String host, int port, String id, String pwd) throws Exception {
		try {
			session = _connect(host, port, id, pwd);
			openChannel(session);
			log.info("[" + getSessionKey() + "]" + " session open.");
			read(null, getDefaultReadTimeout());
		} catch (Exception e) {
			_close();
			throw new Exception(e);
		}
	}

	@Override
	public void connectTunnel(String tHost, int tPort, String tId, String tPwd, String host, int port, String id, String pwd) throws Exception {
		try {
			sessionTunnel = _connect(tHost, tPort, tId, tPwd);
			int lPort = sessionTunnel.setPortForwardingL(0, host, port);
			log.info("[" + getSessionKey() + "]" + " local port forwarding : " + lPort);
			session = _connect("127.0.0.1", lPort, id, pwd);
			openChannel(session);
			log.info("[" + getSessionKey() + "]" + " tunnel session open.");
			read(null, getDefaultReadTimeout());
		} catch (Exception e) {
			_close();
			throw new Exception(e);
		}
	}

	private void openChannel(Session session) throws JSchException, IOException {
		channel = session.openChannel("shell");
		ChannelShell channelShell = (ChannelShell) channel;
		channelShell.setPtyType("vt102", 1000, 24, 1024, 768);

		is = channel.getInputStream();
		os = channel.getOutputStream();
		channel.connect();
	}

	private Properties getConfig() {
		Properties config = new Properties();
		config.put("UserKnownHostsFile", "/dev/null");
		config.put("StrictHostKeyChecking", "no");
		return config;
	}

	private Session _connect(String host, int port, String id, String pwd) throws JSchException {
		JSch jsch = new JSch();
		File privateKeyFile = new File(System.getProperty("user.home") + "/" + ".ssh" + "/" + "id_rsa");
		if(privateKeyFile.exists()) {
			jsch.addIdentity(privateKeyFile.getAbsolutePath());
		}
		Session session = jsch.getSession(id, host, port);
		session.setTimeout(10 * 1000);
		session.setPassword(pwd);
		session.setConfig(getConfig());
		session.connect(10 * 1000);
		return session;
	}

	//parameter option...
	protected abstract String read(String prompt, int timeOut) throws ExecutionException, InterruptedException, TimeoutException, IOException;

	@Override
	public boolean isConnected() {
		return session.isConnected();
	}

	@Override
	public void close() throws IOException {
		try {
			_close();
		} finally {
			log.info("[" + sessionKey + "]" + " session close.");
			SSHSessionManager.INSTANCE.removeSession(sessionKey);
		}
	}

	//Second
	protected int getDefaultReadTimeout() {
		return 10;
	}

	@Override
	public String toString() {
		if(null == name)
			return super.toString() + " {sessionKey='" + sessionKey + '\'' + ", createTime=" + createTime + '}';
		return super.toString() + " {sessionKey='" + sessionKey + '\'' + ", name='" + name + '\'' + ", createTime=" + createTime + '}';
	}

	private void _close() throws IOException {
		if (null != is) {
			is.close();
		}
		if (null != os) {
			os.close();
		}
		if (null != channel) {
			channel.disconnect();
		}
		if (null != sessionTunnel) {
			sessionTunnel.disconnect();
		}
		if (null != session) {
			session.disconnect();
		}
	}
}
