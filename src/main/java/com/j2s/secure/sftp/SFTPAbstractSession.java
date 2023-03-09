package com.j2s.secure.sftp;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Properties;

import com.jcraft.jsch.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "sftp")
abstract class SFTPAbstractSession implements SFTPSession {

	protected Session session = null;
	protected ChannelSftp channelSftp = null;
	protected String name;
	protected String sessionKey;
	protected LocalDateTime createTime = LocalDateTime.now();

	public SFTPAbstractSession(String sessionKey) {
		this.sessionKey = sessionKey;
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
	public void connect(String host, int port, String id, String pwd) throws JSchException, IOException {
		try {
			session = _connect(host, port, id, pwd);
			openChannel(session);
			log.info("[" + getSessionKey() + "]" + " session open.");
		} catch (Exception e) {
			_close();
			throw e;
		}
	}
	
	private void openChannel(Session session) throws JSchException, IOException {
		Channel channel = session.openChannel("sftp");
		channelSftp = (ChannelSftp) channel;
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
		Session session = jsch.getSession(id, host, port);
		session.setTimeout(10 * 1000);
		session.setPassword(pwd);
		session.setConfig(getConfig());
		session.connect(10 * 1000);
		return session;
	}

	@Override
	public boolean isConnected() {
		return session.isConnected();
	}
	
	@Override
	public void close() throws IOException {
		try {
			_close();
		} finally {
			log.debug("[" + sessionKey + "]" + " session close.");
			SFTPSessionManager.INSTANCE.removeSession(sessionKey);
		}
	}

	private void _close() throws IOException {
		if (null != channelSftp) {
			channelSftp.quit();
		}
		if (null != session) {
			session.disconnect();
		}
	}
}
