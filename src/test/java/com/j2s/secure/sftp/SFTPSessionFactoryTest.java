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
    String host = System.getenv().getOrDefault("SSH_TEST_HOST", "127.0.0.1");
    String id = System.getenv().getOrDefault("SSH_TEST_USER", "test-user");
    String pwd = System.getenv().getOrDefault("SSH_TEST_PASSWORD", "change-me");

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
    void openSessionTunnel() {
        String tunnelHost = System.getenv().getOrDefault("SSH_TEST_TUNNEL_HOST", "127.0.0.1");
        String tunnelUser = System.getenv().getOrDefault("SSH_TEST_TUNNEL_USER", "test-user");
        String tunnelPassword = System.getenv().getOrDefault("SSH_TEST_TUNNEL_PASSWORD", "change-me");
        try (SFTPSession sftpSession = SFTPSessionFactory.openSessionTunnel(sessionKey, host, 22, id, pwd, tunnelHost, 22, tunnelUser, tunnelPassword);) {
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