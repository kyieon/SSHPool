package com.j2s.secure.ssh;

import com.j2s.secure.SSHSessionConfig;
import com.j2s.secure.ssh.ex.SSHSessionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.util.UUID;

@Slf4j(topic = "ssh")
public class SSHSyncKeyedPoolableObjectFactory extends BaseKeyedPooledObjectFactory<String, SSHSyncSession> {

    private SSHSessionConfig sshSessionConfig;

    public SSHSyncKeyedPoolableObjectFactory(SSHSessionConfig sshSessionConfig) {
        this.sshSessionConfig = sshSessionConfig;
    }

    @Override
    public SSHSyncSession create(String host) throws Exception {
        String sessionKey = UUID.randomUUID().toString();
        SSHSyncSession session = new SSHSyncSessionImpl(sessionKey);
        session.connect(host, sshSessionConfig.getPort(), sshSessionConfig.getId(), sshSessionConfig.getPwd());
        log.info("[{}][{}] makeObject.", session.getSessionKey(), host);
        return session;
    }

    @Override
    public PooledObject<SSHSyncSession> wrap(SSHSyncSession session) {
        return new DefaultPooledObject<>(session);
    }

    @Override
    public boolean validateObject(String host, PooledObject<SSHSyncSession> p) {
        SSHSyncSession session = p.getObject();
        try {
            synchronized (session) {
                log.info("[{}][{}] validateObject.", session.getSessionKey(), host);
                if (!session.isConnected()) throw new SSHSessionException("session is not connected.");
                session.write("pwd", 3);
                return true;
            }
        } catch (Exception e) {
            log.error("[" + session.getSessionKey() + "][" + host + "] validateObject fail.", e);
        }
        return false;
    }

    @Override
    public void passivateObject(String host, PooledObject<SSHSyncSession> p) throws Exception {
        SSHSyncSession session = p.getObject();
        synchronized (session) {
            log.info("[{}][{}] passivateObject.", session.getSessionKey(), host);
            try {
                session.write("cd ~", 3);
            } catch (Exception e) {
                log.error("[" + session.getSessionKey() + "][" + host + "] passivateObject fail.", e);
                throw e; // Pool closed
            }
        }
    }

    @Override
    public void destroyObject(String host, PooledObject<SSHSyncSession> p) throws Exception {
        SSHSyncSession session = p.getObject();
        synchronized (session) {
            log.info("[{}][{}] destroyObject.", session.getSessionKey(), host);
            session.close();
        }
    }
}