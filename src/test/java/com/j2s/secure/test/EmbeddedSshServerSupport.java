package com.j2s.secure.test;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

/**
 * Apache MINA SSHD embedded server for integration tests.
 * Provides a fake shell (pwd/ls/cd/echo) and a real SFTP subsystem backed by a temp home dir.
 */
public final class EmbeddedSshServerSupport implements AutoCloseable {

    public static final String USER = "testuser";
    public static final String PASS = "testpass";

    private final SshServer sshd;
    private final Path home;

    public EmbeddedSshServerSupport() throws IOException {
        this.home = Files.createTempDirectory("sshpooletest-home");
        Path hostKey = Files.createTempFile("sshpooletest-hostkey", ".ser");
        Files.delete(hostKey);

        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(0); // ephemeral
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKey));
        sshd.setPasswordAuthenticator((username, password, session) ->
                USER.equals(username) && PASS.equals(password));
        sshd.setShellFactory(new TestShellFactory(home));
        sshd.setCommandFactory(new TestShellFactory(home));
        sshd.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        sshd.setFileSystemFactory(new VirtualFileSystemFactory(home));
    }

    public void start() throws IOException {
        sshd.start();
    }

    public int getPort() {
        return sshd.getPort();
    }

    public Path getHome() {
        return home;
    }

    @Override
    public void close() throws IOException {
        sshd.stop(true);
    }
}
