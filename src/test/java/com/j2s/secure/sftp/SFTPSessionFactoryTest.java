package com.j2s.secure.sftp;

import com.j2s.secure.ssh.SSHSessionFactory;
import com.j2s.secure.ssh.SSHSyncSession;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SFTPSessionFactoryTest {

    String sessionKey = UUID.randomUUID().toString();
    String host = "10.180.92.250";
    String id = "ngepc";
    String pwd = "ngepc./";

    @Test
    void openSession() {
        try (SFTPSession sftpSession = SFTPSessionFactory.openSession(sessionKey, host, 22, id, pwd);) {
            List<ChannelSftp.LsEntry> result = sftpSession.ls();
            System.out.println(result);
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SftpException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void openSessionTurnel() {
        try (SFTPSession sftpSession = SFTPSessionFactory.openSessionTurnel(sessionKey, host, 22, id, pwd, "10.180.93.60", 22, "root", "root123");) {
            List<ChannelSftp.LsEntry> result = sftpSession.ls();
            System.out.println(result);
            try (InputStream inputStream = sftpSession.get("/etc/hosts")) {
                List<String> lines = IOUtils.readLines(inputStream, Charset.defaultCharset());
                System.out.println(lines);
            }
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SftpException e) {
            throw new RuntimeException(e);
        }
    }
}