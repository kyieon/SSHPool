package com.j2s.secure.ssh;

import com.j2s.secure.SSHSessionKeyedConfig;
import com.j2s.secure.test.EmbeddedSshServerSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SSHSyncSessionKeyedPoolEmbeddedTest {

    private static EmbeddedSshServerSupport server;
    private SSHSyncSessionKeyedPool pool;

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
        SSHSessionKeyedConfig config = new SSHSessionKeyedConfig();
        config.setHosts(Collections.singletonList("127.0.0.1"));
        config.setPort(server.getPort());
        config.setId(EmbeddedSshServerSupport.USER);
        config.setPwd(EmbeddedSshServerSupport.PASS);
        pool = new SSHSyncSessionKeyedPool(config);
    }

    @AfterEach
    void destroyPool() {
        pool.disconnectAll();
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void execute_returnsOutput_whenHostIsConfigured() throws Exception {
        // Given a keyed pool with the embedded server host registered
        // When executing against that host
        String result = pool.execute("127.0.0.1", session -> {
                    try {
                        return session.write("pwd");
                    } catch (java.io.IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        // Then the output contains the shell home directory
        assertTrue(result.trim().contains(server.getHome().toString()),
                "keyed pool pwd must contain the shell home directory, got: " + result);
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void execute_throws_whenHostIsNotConfigured() {
        // Given a keyed pool without an unknown host
        // When executing against it
        // Then it is rejected
        assertThrows(Exception.class,
                () -> pool.execute("unknown-host", session -> {
                    try {
                        return session.write("pwd");
                    } catch (java.io.IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
    }
}
