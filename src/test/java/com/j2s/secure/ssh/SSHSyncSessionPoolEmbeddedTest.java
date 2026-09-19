package com.j2s.secure.ssh;

import com.j2s.secure.SSHSessionConfig;
import com.j2s.secure.test.EmbeddedSshServerSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SSHSyncSessionPoolEmbeddedTest {

    private static EmbeddedSshServerSupport server;
    private SSHSyncSessionPool pool;

    @BeforeAll
    static void startServer() throws Exception {
        server = new EmbeddedSshServerSupport();
        server.start();
    }

    @AfterAll
    static void stopServer() throws Exception {
        server.close();
    }

    @BeforeEach
    void createPool() {
        SSHSessionConfig config = new SSHSessionConfig();
        config.setHost("127.0.0.1");
        config.setPort(server.getPort());
        config.setId(EmbeddedSshServerSupport.USER);
        config.setPwd(EmbeddedSshServerSupport.PASS);
        pool = new SSHSyncSessionPool(config);
    }

    @AfterEach
    void destroyPool() {
        pool.disconnectAll();
    }

    private String pwd(SSHSyncSession s) {
        try {
            return s.write("pwd");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String echo(SSHSyncSession s, String message) {
        try {
            return s.write("echo " + message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void execute_returnsOutput_whenCommandRunsOnPooledSession() throws Exception {
        // Given a pool against the embedded server
        // When running a command through the pool
        String result = pool.execute(this::pwd);
        // Then pooled validation (pwd) and the command both succeed
        assertNotNull(result);
        assertTrue(result.trim().contains(server.getHome().toString()),
                "pooled pwd must contain the shell home directory, got: " + result);
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void executeOnce_returnsOutput_andInvalidatesSession() throws Exception {
        // Given a pool
        // When running a one-shot command
        String result = pool.executeOnce(session -> echo(session, "one-shot"));
        // Then the result is returned
        assertNotNull(result);
        assertTrue(result.trim().contains("one-shot"), "echo output must be returned, got: " + result);
        // And a subsequent one-shot still works (fresh session created)
        String again = pool.executeOnce(session -> echo(session, "again"));
        assertTrue(again.trim().contains("again"), "second one-shot must work, got: " + again);
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void execute_reusesIdleSession_acrossInvocations() throws Exception {
        // Given a pool
        // When executing two commands sequentially
        String first = pool.execute(session -> echo(session, "first"));
        String second = pool.execute(session -> echo(session, "second"));
        // Then both succeed (borrow -> validate -> run -> passivate -> return)
        assertTrue(first.trim().contains("first"));
        assertTrue(second.trim().contains("second"));
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void execute_throws_whenCredentialsAreWrong() {
        // Given a pool with bad credentials
        SSHSessionConfig bad = new SSHSessionConfig();
        bad.setHost("127.0.0.1");
        bad.setPort(server.getPort());
        bad.setId(EmbeddedSshServerSupport.USER);
        bad.setPwd("wrong-password");
        try (SSHSyncSessionPool badPool = new SSHSyncSessionPool(bad)) {
            // When executing
            // Then it fails
            assertThrows(Exception.class, () -> badPool.execute(this::pwd));
        }
    }
}
