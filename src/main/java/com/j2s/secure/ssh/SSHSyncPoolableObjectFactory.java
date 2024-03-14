package com.j2s.secure.ssh;

import com.j2s.secure.SSHSessionConfig;
import com.j2s.secure.ssh.ex.SSHSessionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.util.UUID;

@Slf4j(topic = "ssh")
public class SSHSyncPoolableObjectFactory extends BasePooledObjectFactory<SSHSyncSession> {

	private SSHSessionConfig sshSessionConfig;

	public SSHSyncPoolableObjectFactory(SSHSessionConfig sshSessionConfig) {
		this.sshSessionConfig = sshSessionConfig;
	}

	@Override
	public SSHSyncSession create() throws Exception {
		String sessionKey = UUID.randomUUID().toString();
		SSHSyncSession session = null;
		try {
			session = new SSHSyncSessionImpl(sessionKey);
			session.connect(sshSessionConfig.getHost(), sshSessionConfig.getPort(), sshSessionConfig.getId(), sshSessionConfig.getPwd());
			log.info("[{}] makeObject.", session.getSessionKey());
			return session;
		} catch (Exception e) {
			if(null != session)
				session.close();
			throw e;
		}
	}

	@Override
	public PooledObject<SSHSyncSession> wrap(SSHSyncSession session) {
		return new DefaultPooledObject<>(session);
	}

	@Override
	public boolean validateObject(PooledObject<SSHSyncSession> p) {
		SSHSyncSession session = p.getObject();
		try {
			synchronized (session) {
				log.info("[{}] validateObject.", session.getSessionKey());
				if(!session.isConnected())
					throw new SSHSessionException("session is not connected.");
				session.write("pwd", 3);
				return true;
			}
		} catch (Exception e) {
			log.error("[" + session.getSessionKey() + "] validateObject fail.", e);
		}
		return false;
	}

	@Override
	public void passivateObject(PooledObject<SSHSyncSession> p) throws Exception {
		SSHSyncSession session = p.getObject();
		synchronized (session) {
			log.info("[{}] passivateObject.", session.getSessionKey());
			try {
				session.write("cd ~", 3);
			} catch (Exception e) {
				log.error("[" + session.getSessionKey() + "] passivateObject fail.", e);
				throw e; // Pool closed
			}
		}
	}

	@Override
	public void destroyObject(PooledObject<SSHSyncSession> p) throws Exception {
		SSHSyncSession session = p.getObject();
		synchronized (session) {
			log.info("[{}] destroyObject.", session.getSessionKey());
			session.close();
		}
	}
}