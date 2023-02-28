//package com.j2s.secure.ssh;
//
//import com.j2s.secure.SSHSessionConfig;
//import com.j2s.secure.ssh.ex.SSHSessionException;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
//import org.apache.commons.pool2.PooledObject;
//
//import java.util.UUID;
//
//@Slf4j(topic = "ssh")
//public class SSHSyncKeyedPoolableObjectFactory extends BaseKeyedPooledObjectFactory<String, SSHSyncSession> {
//
//	private SSHSessionConfig sshSessionConfig;
//
//	public SSHSyncKeyedPoolableObjectFactory(SSHSessionConfig sshSessionConfig) {
//		this.sshSessionConfig = sshSessionConfig;
//	}
//
//	public SSHSessionConfig getSshSessionConfig() {
//		return sshSessionConfig;
//	}
//
//	@Override
//	public SSHSyncSession create(String host) throws Exception {
//		String sessionKey = UUID.randomUUID().toString();
//		SSHSyncSession session = new SSHSyncSessionImpl(sessionKey);
//		session.connect(host, sshSessionConfig.getPort(), sshSessionConfig.getId(), sshSessionConfig.getPwd());
//		return session;
//	}
//
//	@Override
//	public PooledObject<SSHSyncSession> makeObject(String key) throws Exception {
//		return super.makeObject(key);
//	}
//
//	@Override
//	public PooledObject<SSHSyncSession> wrap(SSHSyncSession sshSyncSession) {
//		return null;
//	}
//
//	@Override
//	public void destroyObject(Object key, Object obj) throws Exception {
//		SSHSyncSession session = convertSessionObj(obj);
//		synchronized (session) {
//			log.info("[{}][" + session.getSessionKey() + "] destroyObject.", key);
//			session.close();
//		}
//	}
//
//	@Override
//	public boolean validateObject(Object key, Object obj) {
//		SSHSyncSession session = null;
//		try {
//			session = convertSessionObj(obj);
//			synchronized (session) {
//				log.info("[{}][" + session.getSessionKey() + "] validateObject.", key);
//				if(!session.isConnected())
//					throw new SSHSessionException(String.format("[%s] session is not connected.", key));
//				session.write("pwd", 3);
//				return true;
//			}
//		} catch (Exception e) {
//			String errorMsg = "";
//			if(null != session) {
//				errorMsg = String.format("[%s][" + session.getSessionKey() + "] validateObject fail.", key);
//			}
//			log.error(errorMsg, e);
//		}
//		return false;
//	}
//
//	@Override
//	public void passivateObject(Object key, Object obj) throws Exception {
//		SSHSyncSession session = convertSessionObj(obj);
//		synchronized (session) {
//			log.info("[{}][" + session.getSessionKey() + "] passivateObject.", key);
//			try {
//				session.write("cd ~", 3);
//			} catch (Exception e) {
//				throw new IllegalStateException(e); // Pool closed
//			}
//		}
//	}
//
//	protected SSHSyncSession convertSessionObj(Object obj) throws SSHSessionException {
//		if(obj instanceof SSHSyncSession) {
//			return (SSHSyncSession) obj;
//		}
//		throw new SSHSessionException("class cast error. " + obj.getClass().getName());
//	}
//}