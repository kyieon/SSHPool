package com.j2s.secure.test;

import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.command.CommandFactory;
import org.apache.sshd.server.shell.ShellFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Fake interactive shell: answers each line, then prints a "$ " prompt.
 * The SSHPool client treats a trailing "$" as end-of-output.
 */
public final class TestShellFactory implements ShellFactory, CommandFactory {

    private final Path home;

    public TestShellFactory(Path home) {
        this.home = home;
    }

    @Override
    public Command createShell(ChannelSession channel) throws IOException {
        return new TestShell(home, "shell");
    }

    @Override
    public Command createCommand(ChannelSession channel, String command) {
        return new TestShell(home, command);
    }

    static final class TestShell implements Command {

        private final Path home;
        private Path cwd;
        private InputStream in;
        private OutputStream out;
        private ExitCallback callback;

        TestShell(Path home, String name) {
            this.home = home;
            this.cwd = home;
        }

        @Override
        public void setInputStream(InputStream in) {
            this.in = in;
        }

        @Override
        public void setOutputStream(OutputStream out) {
            this.out = out;
        }

        @Override
        public void setErrorStream(OutputStream err) {
            // unused
        }

        @Override
        public void setExitCallback(ExitCallback callback) {
            this.callback = callback;
        }

        @Override
        public void start(ChannelSession channel, Environment env) {
            Thread t = new Thread(this::runLoop, "test-shell");
            t.setDaemon(true);
            t.start();
        }

        @Override
        public void destroy(ChannelSession channel) {
            if (callback != null) {
                callback.onExit(0);
            }
        }

        private void runLoop() {
            PrintWriter w = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true);
            w.print("$ ");
            w.flush();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    handle(line, w);
                }
            } catch (IOException e) {
                // client closed the channel
            }
            if (callback != null) {
                callback.onExit(0);
            }
        }

        private void handle(String line, PrintWriter w) throws IOException {
            String cmd = line.trim();
            if (cmd.isEmpty()) {
                prompt(w);
                return;
            }
            String out;
            if ("pwd".equals(cmd)) {
                out = cwd.toString();
            } else if ("cd ~".equals(cmd) || "cd".equals(cmd)) {
                cwd = home;
                out = null;
            } else if ("ll".equals(cmd) || "ls".equals(cmd)) {
                try (Stream<Path> st = Files.list(cwd)) {
                    out = st.map(p -> p.getFileName().toString())
                            .sorted()
                            .collect(Collectors.joining("\n"));
                }
            } else if (cmd.startsWith("echo ")) {
                out = cmd.substring("echo ".length());
            } else {
                out = "";
            }
            if (out != null && !out.isEmpty()) {
                w.println(out);
            }
            prompt(w);
        }

        private void prompt(PrintWriter w) {
            w.print("$ ");
            w.flush();
        }
    }
}
