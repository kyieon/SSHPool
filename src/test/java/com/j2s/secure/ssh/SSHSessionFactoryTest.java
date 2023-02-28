package com.j2s.secure.ssh;

import com.jcraft.jsch.JSchException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SSHSessionFactoryTest {


    String sessionKey = UUID.randomUUID().toString();
    String host = "10.180.92.250";
    String id = "ngepc";
    String pwd = "ngepc./";

    @Test
    void openSyncSession() {
        try (SSHSyncSession sshSyncSession = SSHSessionFactory.openSyncSession(sessionKey, host, 22, id, pwd);) {
            String result = sshSyncSession.write("ll");
            System.out.println(result);
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void openAsyncSession() {
    }
}