package com.j2s.secure.ssh;

import com.jcraft.jsch.JSchException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

@Disabled("requires a live SSH server with hardcoded credentials")
class SSHSessionFactoryTest {



    String sessionKey = UUID.randomUUID().toString();
    String host = System.getenv().getOrDefault("SSH_TEST_HOST", "127.0.0.1");
    String id = System.getenv().getOrDefault("SSH_TEST_USER", "test-user");
    String pwd = System.getenv().getOrDefault("SSH_TEST_PASSWORD", "change-me");

    @Test
    void openSyncSession() {
        try (SSHSyncSession session = SSHSessionFactory.openSyncSession(sessionKey, host, 22, id, pwd, "a", "b", "c");) {
            String result = session.write("ll");
            System.out.println(result);
            Thread.sleep(20*1000L);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void openAsyncSession() {
        try (SSHAsyncSession session = SSHSessionFactory.openAsyncSession(sessionKey, host, 22, id, pwd);) {
            session.onTrigger(m -> {
                System.out.println(m);
            });
            session.write("ll");
            session.write("pwd");
            Thread.sleep(10*1000L);
            session.write("pwd");
            Thread.sleep(3*1000L);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}