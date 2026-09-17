package com.j2s.secure.sftp;

import com.j2s.secure.test.EmbeddedSshServerSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SFTPSessionFactoryEmbeddedTest {

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

    private SFTPSession open() throws IOException {
        try {
            return SFTPSessionFactory.openSession(UUID.randomUUID().toString(), "127.0.0.1",
                    server.getPort(), EmbeddedSshServerSupport.USER, EmbeddedSshServerSupport.PASS);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("failed to open sftp session", e);
        }
    }

    private List<String> names(List<com.jcraft.jsch.ChannelSftp.LsEntry> entries) {
        return entries.stream()
                .map(com.jcraft.jsch.ChannelSftp.LsEntry::getFilename)
                .collect(Collectors.toList());
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void put_and_ls_and_cat_roundTrip() throws Exception {
        // Given a connected SFTP session and a local file
        try (SFTPSession sftp = open()) {
            Path local = Files.createTempFile("sshpooletest-upload", ".txt");
            Files.write(local, "hello sftp".getBytes(StandardCharsets.UTF_8));
            String remoteName = "uploaded-" + UUID.randomUUID() + ".txt";
            // When uploading
            sftp.put(local.toFile(), remoteName);
            // Then listing shows the file and cat reads it back
            assertTrue(names(sftp.ls("/")).contains(remoteName),
                    "uploaded file must appear in ls");
            assertEquals("hello sftp", sftp.cat("/" + remoteName));
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void get_returnsFileContent_whenFileExists() throws Exception {
        // Given a remote file placed directly on the server fs
        try (SFTPSession sftp = open()) {
            Path remote = server.getHome().resolve("direct.txt");
            Files.write(remote, "direct content".getBytes(StandardCharsets.UTF_8));
            // When reading it via get() (SFTP root is the home dir)
            String content;
            try (java.io.InputStream is = sftp.get("/direct.txt")) {
                content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            // Then the content matches
            assertEquals("direct content", content);
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void rename_movesFile_whenDestinationIsNew() throws Exception {
        // Given an uploaded file
        try (SFTPSession sftp = open()) {
            Path remote = server.getHome().resolve("before-" + UUID.randomUUID() + ".txt");
            Files.write(remote, "renamed".getBytes(StandardCharsets.UTF_8));
            String src = "/" + remote.getFileName();
            String dst = "/after.txt";
            // When renaming
            sftp.rename(src, dst);
            // Then only the new name exists
            List<String> names = names(sftp.ls("/"));
            assertFalse(names.contains(remote.getFileName().toString()), "source must be gone");
            assertTrue(names.contains("after.txt"), "destination must exist");
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void rm_removesFile_whenFileExists() throws Exception {
        // Given a remote file
        try (SFTPSession sftp = open()) {
            Path remote = server.getHome().resolve("doomed.txt");
            Files.write(remote, "bye".getBytes(StandardCharsets.UTF_8));
            assertTrue(Files.exists(remote));
            // When deleting (SFTP root is the home dir)
            sftp.rm("/" + remote.getFileName());
            // Then the file is gone
            assertFalse(Files.exists(remote));
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void rm_removesDirectoryRecursively_whenItHasFiles() throws Exception {
        // Given a directory with a nested file
        try (SFTPSession sftp = open()) {
            Path dir = server.getHome().resolve("dir-" + UUID.randomUUID());
            Files.createDirectories(dir);
            Files.write(dir.resolve("nested.txt"), "nested".getBytes(StandardCharsets.UTF_8));
            // When deleting the directory (SFTP root is the home dir)
            sftp.rm("/" + dir.getFileName());
            // Then the whole tree is gone
            assertFalse(Files.exists(dir), "directory must be removed recursively");
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void pwd_and_cd_navigateWithinServer() throws Exception {
        // Given a session at the server home
        try (SFTPSession sftp = open()) {
            // When asking for pwd
            String pwd = sftp.pwd();
            // Then it reports the SFTP root ("/" maps to the home dir)
            assertEquals("/", pwd);
        }
    }
}
