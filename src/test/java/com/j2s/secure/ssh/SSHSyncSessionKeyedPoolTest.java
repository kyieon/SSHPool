package com.j2s.secure.ssh;

import com.j2s.secure.SSHSessionKeyedConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

class SSHSyncSessionKeyedPoolTest {

    private SSHSyncSessionKeyedPool pool;

    @BeforeEach
    void before() {
        SSHSessionKeyedConfig config = new SSHSessionKeyedConfig();
        config.setHosts(Arrays.asList("10.180.92.250"));
        config.setPort(22);
        config.setId("ngepc");
        config.setPwd("ngepc./");

        pool = new SSHSyncSessionKeyedPool(config);
    }

    @AfterEach
    void after() {
        pool.disconnectAll();
    }

    @Test
    void execute() throws Exception {
        String result = this.pool.execute("10.180.92.250", (session) -> {
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