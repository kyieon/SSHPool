package com.j2s.secure.ssh;

import com.j2s.secure.SSHSessionConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class SSHSyncSessionPoolTest {

    private SSHSyncSessionPool sshSyncSessionPool;

    @BeforeEach
    void beforeAll() {
        SSHSessionConfig config = new SSHSessionConfig();
        config.setHost("10.180.92.250");
        config.setPort(22);
        config.setId("ngepc");
        config.setPwd("ngepc./");

        sshSyncSessionPool = new SSHSyncSessionPool(config);
    }

    @Test
    void execute() throws Exception {
        String result = this.sshSyncSessionPool.execute((session) -> {
            try {
                return session.write("ll");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println(result);

        Thread.sleep(1 * 1000);
    }

    @Test
    void executeOnce() throws Exception {
        String result = this.sshSyncSessionPool.executeOnce((session) -> {
            try {
                return session.write("ll");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println(result);

        Thread.sleep(1 * 1000);
    }
}