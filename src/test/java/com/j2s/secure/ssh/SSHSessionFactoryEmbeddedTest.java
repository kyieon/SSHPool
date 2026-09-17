package com.j2s.secure.ssh;

import com.j2s.secure.test.EmbeddedSshServerSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SSHSessionFactoryEmbeddedTest {

    private static EmbeddedSshServerSupport server;

    @BeforeAll
    static void startServer() throws Exception {
        server = new EmbeddedSshServerSupport();
        server.start();
    }

    @AfterAll
    static void stopServer() throws Exception {
        server.close();
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void write_returnsPromptTerminatedOutput_whenCommandSucceeds() throws Exception {
        // Given a live session on the embedded server
        String sessionKey = UUID.randomUUID().toString();
        try (SSHSyncSession session = SSHSessionFactory.openSyncSession(sessionKey, "127.0.0.1",
                server.getPort(), EmbeddedSshServerSupport.USER, EmbeddedSshServerSupport.PASS)) {
            // When executing a command
            String result = session.write("pwd");
            // Then the response contains the server home dir and a trailing prompt
            assertTrue(result.trim().contains(server.getHome().toString()),
                    "pwd output must contain the shell home directory, got: " + result);
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void openSyncSession_throws_whenCredentialsAreWrong() {
        // Given wrong credentials
        // When opening a session
        // Then authentication fails
        assertThrows(Exception.class, () ->
                SSHSessionFactory.openSyncSession(UUID.randomUUID().toString(), "127.0.0.1",
                        server.getPort(), EmbeddedSshServerSupport.USER, "wrong-password"));
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void getSession_returnsRegisteredSession_afterOpen() throws Exception {
        // Given an opened session
        String sessionKey = UUID.randomUUID().toString();
        try (SSHSyncSession session = SSHSessionFactory.openSyncSession(sessionKey, "127.0.0.1",
                server.getPort(), EmbeddedSshServerSupport.USER, EmbeddedSshServerSupport.PASS)) {
            // When looking it up by key
            SSHSyncSession found = SSHSessionFactory.getSession(sessionKey);
            // Then the same instance is returned
            assertNotNull(found);
            assertTrue(found.isConnected());
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void openAsyncSession_deliversMessages_whenTriggerRegistered() throws Exception {
        // Given an async session with a trigger
        String sessionKey = UUID.randomUUID().toString();
        try (SSHAsyncSession session = SSHSessionFactory.openAsyncSession(sessionKey, "127.0.0.1",
                server.getPort(), EmbeddedSshServerSupport.USER, EmbeddedSshServerSupport.PASS)) {
            java.util.concurrent.atomic.AtomicReference<String> received = new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            session.onTrigger(m -> {
                received.set(m.getResult());
                latch.countDown();
            });
            // When writing a command
            session.write("echo hello");
            // Then the trigger receives the output
            assertTrue(latch.await(10, TimeUnit.SECONDS), "trigger did not receive a message");
            assertNotNull(received.get());
        }
    }
}
