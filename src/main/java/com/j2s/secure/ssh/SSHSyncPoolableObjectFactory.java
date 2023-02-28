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

	protected SSHSyncPoolableObjectFactory(SSHSessionConfig sshSessionConfig) {
		this.sshSessionConfig = sshSessionConfig;
	}
	
	public SSHSessionConfig getSshSessionConfig() {
		return sshSessionConfig;
	}

	@Override
	public SSHSyncSession create() throws Exception {
		String sessionKey = UUID.randomUUID().toString();
		SSHSyncSession session = new SSHSyncSessionImpl(sessionKey);
		session.connect(sshSessionConfig.getHost(), sshSessionConfig.getPort(), sshSessionConfig.getId(), sshSessionConfig.getPwd());
		log.info("[" + session.getSessionKey() + "][" + session.toString() + "] makeObject.");
		return session;
	}

	@Override
	public PooledObject<SSHSyncSession> wrap(SSHSyncSession o) {
		return new DefaultPooledObject<>(o);
	}

	@Override
	public void destroyObject(PooledObject<SSHSyncSession> p) throws Exception {
		SSHSyncSession session = p.getObject();
		synchronized (session) {
			log.info("[" + session.getSessionKey() + "][" + session.toString() + "] destroyObject.");
			session.close();
		}
	}

	@Override
	public boolean validateObject(PooledObject<SSHSyncSession> p) {
		SSHSyncSession session = p.getObject();
		try {
			synchronized (session) {
				log.info("[" + session.getSessionKey() + "][" + session.toString() + "] validateObject.");
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
			log.info("[" + session.getSessionKey() + "][" + session.toString() + "] passivateObject.");
			try {
				session.write("cd ~", 3);
			} catch (Exception e) {
				throw e; // Pool closed
			}
		}
	}
}