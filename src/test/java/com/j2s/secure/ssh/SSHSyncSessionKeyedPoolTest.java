package com.j2s.secure.ssh;

import com.j2s.secure.SSHSessionKeyedConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

class SSHSyncSessionKeyedPoolTest {

    private SSHSyncSessionKeyedPool pool;
    private String host = System.getenv().getOrDefault("SSH_TEST_HOST", "127.0.0.1");

    @BeforeEach
    void before() {
        SSHSessionKeyedConfig config = new SSHSessionKeyedConfig();
        config.setHosts(Arrays.asList(host));
        config.setPort(22);
        config.setId(System.getenv().getOrDefault("SSH_TEST_USER", "test-user"));
        config.setPwd(System.getenv().getOrDefault("SSH_TEST_PASSWORD", "change-me"));

        pool = new SSHSyncSessionKeyedPool(config);
    }

    @AfterEach
    void after() {
        pool.disconnectAll();
    }

    @Test
    void execute() throws Exception {
        String result = this.pool.execute(host, (session) -> {
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