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

}
