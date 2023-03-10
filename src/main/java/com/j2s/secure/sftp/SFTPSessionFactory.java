package com.j2s.secure.sftp;

import com.jcraft.jsch.JSchException;

import java.io.IOException;

public class SFTPSessionFactory {

    public static SFTPSession openSession(String sessionKey, String host, int port, String id, String pwd) throws JSchException, IOException {
        SFTPSessionImpl sftpSession  = new SFTPSessionImpl(sessionKey);
        sftpSession.connect(host, port, id, pwd);
        SFTPSessionManager.INSTANCE.putSession(sessionKey, sftpSession);
        return sftpSession;
    }

    /**
     * @param sessionKey	- Session Key
     * @param tHost			- Turnel Server host
     * @param tPort			- Turnel Server port
     * @param tId			- Turnel Server Id
     * @param tPwd			- Turnel server Pwd
     * @param host			- Server host
     * @param port			- Server Port
     * @param id			- Server Id
     * @param pwd			- Server Pwd
     */
    public static SFTPSession openSessionTurnel(String sessionKey, String tHost, int tPort, String tId, String tPwd, String host, int port, String id, String pwd) throws JSchException, IOException {
        SFTPSession sftpSession = new SFTPSessionImpl(sessionKey);
        sftpSession.connectTunnel(tHost, tPort, tId, tPwd, host, port, id, pwd);
        SFTPSessionManager.INSTANCE.putSession(sessionKey, sftpSession);
        return sftpSession;
    }

}
